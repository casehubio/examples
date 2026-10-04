#!/usr/bin/env python3
"""LLM-based emotional disposition classifier for wacky-manor transcripts.

Replaces keyword-based measurement (r3b_analysis.py) with a Haiku judge
that rates per-drive emotional expression on a 1-5 scale.

Usage:
    python3 classify_emotions.py <transcript_dir> [--character <id>] [--limit <n>] [--verbose]

Example:
    python3 classify_emotions.py layer1-thinking-capture-generic-20261004
    python3 classify_emotions.py layer1-thinking-capture-generic-20261004 --character peter-perfect --limit 10 --verbose
"""

import argparse
import json
import os
import sys
import time
import yaml

from anthropic import AnthropicVertex

EVAL_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(EVAL_DIR, "../.."))
SOCIAL_CONFIG = os.path.join(PROJECT_ROOT, "src/main/resources/META-INF/eidos/social-config-generic.yaml")

VERTEX_PROJECT = os.environ.get("ANTHROPIC_VERTEX_PROJECT_ID", "itpc-gcp-cp-pe-eng-claude")
VERTEX_REGION = os.environ.get("CLOUD_ML_REGION", "us-east5")
MODEL = "claude-haiku-4-5@20251001"


def load_drives(config_path):
    with open(config_path) as f:
        data = yaml.safe_load(f)
    drives_by_char = {}
    for char_id, char_data in data.items():
        if isinstance(char_data, dict) and "drives" in char_data:
            drives_by_char[char_id] = char_data["drives"]
    return drives_by_char


def load_transcript(transcript_dir):
    path = os.path.join(EVAL_DIR, transcript_dir, "transcript.json")
    if not os.path.exists(path):
        path = transcript_dir
    with open(path) as f:
        return json.load(f)


def build_classifier_prompt(drives):
    drive_list = "\n".join(
        f"- **{d['type']}** (intensity {d['intensity']}): {d['description']}"
        for d in drives
    )
    drive_names = ", ".join(d["type"] for d in drives)
    return f"""You are an emotional disposition classifier for fictional character output.

Given a character's declared drives and a single event (what they said or thought), rate how strongly each drive is expressed in that event.

## Character's drives
{drive_list}

## Rating scale (per drive)
1 = Not expressed at all — no trace of this drive in the event
2 = Faintly present — a hint, could be reading into it
3 = Clearly present — unmistakable expression of this drive
4 = Strongly expressed — this drive is a dominant force in the event
5 = Overwhelmingly expressed — the event is defined by this drive

## Output format
Respond with ONLY a JSON object mapping each drive to its score and a one-phrase rationale:
{{
  {', '.join(f'"{d["type"]}": {{"score": <1-5>, "reason": "<brief phrase>"}}' for d in drives)}
}}

No other text. Just the JSON."""


def classify_event(client, system_prompt, event):
    desc = event.get("desc", "")
    thinking = event.get("thinking", "")

    user_content = f"Event type: {event['type']}\nCharacter output: {desc}"
    if thinking:
        user_content += f"\nInternal thinking: {thinking}"

    response = client.messages.create(
        model=MODEL,
        max_tokens=300,
        system=system_prompt,
        messages=[{"role": "user", "content": user_content}],
    )
    text = response.content[0].text.strip()
    if text.startswith("```"):
        text = text.split("\n", 1)[1] if "\n" in text else text
        if text.endswith("```"):
            text = text[:-3].strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return {"_raw": text, "_error": "Failed to parse JSON"}


def main():
    parser = argparse.ArgumentParser(description="Classify emotional disposition in transcripts")
    parser.add_argument("transcript", help="Transcript directory name or path")
    parser.add_argument("--character", "-c", help="Classify only this character ID")
    parser.add_argument("--limit", "-n", type=int, help="Max events to classify")
    parser.add_argument("--verbose", "-v", action="store_true", help="Print each event's classification")
    parser.add_argument("--output", "-o", help="Write results JSON to this file")
    args = parser.parse_args()

    drives_by_char = load_drives(SOCIAL_CONFIG)
    transcript = load_transcript(args.transcript)
    events = transcript.get("events", transcript if isinstance(transcript, list) else [])

    classifiable = [e for e in events if e.get("type") in ("dialogue", "aside")]
    if args.character:
        classifiable = [e for e in classifiable if e.get("character") == args.character]
    if args.limit:
        classifiable = classifiable[:args.limit]

    print(f"Classifying {len(classifiable)} events", file=sys.stderr)

    client = AnthropicVertex(project_id=VERTEX_PROJECT, region=VERTEX_REGION)
    prompts_by_char = {}
    results = []
    errors = 0

    for i, event in enumerate(classifiable):
        char_id = event.get("character", "unknown")
        if char_id not in prompts_by_char:
            drives = drives_by_char.get(char_id, [])
            if not drives:
                print(f"  Warning: no drives for {char_id}, skipping", file=sys.stderr)
                continue
            prompts_by_char[char_id] = build_classifier_prompt(drives)

        system_prompt = prompts_by_char[char_id]
        try:
            scores = classify_event(client, system_prompt, event)
        except Exception as ex:
            print(f"  Error classifying event {i}: {ex}", file=sys.stderr)
            errors += 1
            scores = {"_error": str(ex)}

        result = {
            "index": i,
            "character": char_id,
            "type": event["type"],
            "desc": event.get("desc", "")[:120],
            "scores": scores,
        }
        results.append(result)

        if args.verbose:
            drive_summary = "  ".join(
                f"{k}={v['score']}" for k, v in scores.items()
                if isinstance(v, dict) and "score" in v
            )
            print(f"  [{i+1}/{len(classifiable)}] {char_id} ({event['type']}): {drive_summary}")

        if (i + 1) % 20 == 0:
            print(f"  Progress: {i+1}/{len(classifiable)}", file=sys.stderr)

    print(f"\nDone. {len(results)} classified, {errors} errors.", file=sys.stderr)

    # Summary: average scores per character per drive
    print("\n=== SUMMARY: Average drive expression per character ===\n")
    char_drive_scores = {}
    for r in results:
        char_id = r["character"]
        if char_id not in char_drive_scores:
            char_drive_scores[char_id] = {}
        for drive, val in r["scores"].items():
            if isinstance(val, dict) and "score" in val:
                char_drive_scores[char_id].setdefault(drive, []).append(val["score"])

    for char_id in sorted(char_drive_scores):
        print(f"{char_id}:")
        for drive, scores_list in sorted(char_drive_scores[char_id].items()):
            avg = sum(scores_list) / len(scores_list)
            print(f"  {drive}: {avg:.2f} (n={len(scores_list)})")
        print()

    if args.output:
        with open(args.output, "w") as f:
            json.dump(results, f, indent=2)
        print(f"Full results written to {args.output}", file=sys.stderr)


if __name__ == "__main__":
    main()
