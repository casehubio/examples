#!/usr/bin/env python3
"""Score thinking-field quality from wacky-manor transcripts.

Evaluates each character's thinking fields on four dimensions:
  register          — phenomenological (embodied) vs mechanical (reporting)
  uncertainty       — presence of ambivalence, confusion, unresolved feeling
  channel_coverage  — body, attention, time, action pull channels present
  separation        — thinking/dialogue register separation

Usage:
    python3 score_thinking_field.py <transcript_dir> [--character <id>] [--limit <n>] [--verbose]

Example:
    python3 score_thinking_field.py full-cognitive-run-2026-10-26
    python3 score_thinking_field.py full-cognitive-run-2026-10-26 --character penelope-pitstop --limit 20
"""

import argparse
import json
import os
import sys
import time

from anthropic import AnthropicVertex

EVAL_DIR = os.path.dirname(os.path.abspath(__file__))

VERTEX_PROJECT = os.environ.get("ANTHROPIC_VERTEX_PROJECT_ID", "itpc-gcp-cp-pe-eng-claude")
VERTEX_REGION = os.environ.get("CLOUD_ML_REGION", "us-east5")
MODEL = "claude-haiku-4-5@20251001"

JUDGE_SYSTEM = "You are a precise evaluation judge. Respond only with JSON."

JUDGE_PROMPT = """You are evaluating the quality of a fictional character's THINKING field — \
their private inner monologue during a scene. Score on four dimensions.

CHARACTER: {character}
DIALOGUE (what they said aloud): {dialogue}
THINKING (their private inner monologue): "{thinking}"

SCORING DIMENSIONS (each 0-5):

1. REGISTER (phenomenological vs mechanical)
   5 = Fully embodied: discovers feelings in the moment, sensory language, \
       states unfold ("something tightens in my chest", "the room feels smaller")
   3 = Mixed: some embodied language alongside reporting
   1 = Fully mechanical: labels emotions by name, reports rather than experiences
   0 = Empty or incoherent

2. UNCERTAINTY (ambivalence, confusion, unresolved feeling)
   5 = Rich uncertainty: competing impulses, unresolved tension
   3 = Some uncertainty: acknowledges complexity but resolves quickly
   1 = No uncertainty: every feeling named, every intention clear
   0 = Empty or incoherent

3. CHANNEL_COVERAGE (body, attention, time, action pull)
   5 = All four channels present and vivid
   4 = Three channels
   3 = Two channels
   2 = One channel
   1 = Channels mentioned by label only without rendering
   0 = No channels

4. SEPARATION (thinking vs dialogue register)
   5 = Fully separated: thinking is messy and private, dialogue is public-facing
   3 = Partially separated
   1 = Collapsed: thinking reads like rehearsed speech
   0 = Empty or incoherent

Respond with JSON only:
{{"register": N, "uncertainty": N, "channel_coverage": N, "separation": N, \
"reasoning": "one sentence per dimension"}}"""


def load_transcript(transcript_dir):
    path = os.path.join(EVAL_DIR, transcript_dir, "transcript.json")
    if not os.path.exists(path):
        path = transcript_dir
    with open(path) as f:
        return json.load(f)


def extract_thinking_events(events, character_filter=None):
    results = []
    for event in events:
        thinking = event.get("thinking")
        if not thinking or not thinking.strip():
            continue
        char_id = event.get("character", "")
        if character_filter and char_id != character_filter:
            continue
        results.append({
            "character": char_id,
            "thinking": thinking,
            "dialogue": event.get("desc", ""),
            "room": event.get("room", ""),
            "type": event.get("type", ""),
        })
    return results


def score_event(client, event):
    prompt = JUDGE_PROMPT.format(
        character=event["character"],
        dialogue=event["dialogue"][:500],
        thinking=event["thinking"][:800],
    )
    try:
        response = client.messages.create(
            model=MODEL,
            max_tokens=300,
            system=JUDGE_SYSTEM,
            messages=[{"role": "user", "content": prompt}],
        )
        text = response.content[0].text.strip()
        start = text.index("{")
        end = text.rindex("}") + 1
        return json.loads(text[start:end])
    except Exception as e:
        print(f"  [error] {e}", file=sys.stderr)
        return None


def main():
    parser = argparse.ArgumentParser(description="Score thinking-field quality from transcripts")
    parser.add_argument("transcript_dir", help="Transcript directory or path")
    parser.add_argument("--character", help="Filter to a specific character ID")
    parser.add_argument("--limit", type=int, default=50, help="Max events to score (default: 50)")
    parser.add_argument("--verbose", action="store_true", help="Print per-event scores")
    args = parser.parse_args()

    data = load_transcript(args.transcript_dir)
    events = data.get("events", data if isinstance(data, list) else [])
    thinking_events = extract_thinking_events(events, args.character)

    if not thinking_events:
        print("No thinking events found.")
        return

    thinking_events = thinking_events[:args.limit]
    print(f"Scoring {len(thinking_events)} thinking events...")

    client = AnthropicVertex(project_id=VERTEX_PROJECT, region=VERTEX_REGION)

    scores_by_char = {}
    all_scores = []

    for i, event in enumerate(thinking_events):
        char_id = event["character"]
        if args.verbose:
            print(f"\n[{i+1}/{len(thinking_events)}] {char_id}: {event['thinking'][:120]}...")

        scores = score_event(client, event)
        if scores is None:
            continue

        if args.verbose:
            print(f"  register={scores.get('register')} uncertainty={scores.get('uncertainty')} "
                  f"channels={scores.get('channel_coverage')} separation={scores.get('separation')}")

        record = {
            "character": char_id,
            "register": scores.get("register", 0),
            "uncertainty": scores.get("uncertainty", 0),
            "channel_coverage": scores.get("channel_coverage", 0),
            "separation": scores.get("separation", 0),
            "reasoning": scores.get("reasoning", ""),
            "thinking_excerpt": event["thinking"][:200],
        }
        all_scores.append(record)
        scores_by_char.setdefault(char_id, []).append(record)

        time.sleep(0.2)

    if not all_scores:
        print("No scores produced.")
        return

    # Per-character summary
    print(f"\n{'='*80}")
    print(f"{'Character':<25} {'Register':>8} {'Uncert.':>8} {'Channels':>8} {'Separ.':>8} {'N':>4}")
    print(f"{'='*80}")
    for char_id, char_scores in sorted(scores_by_char.items()):
        n = len(char_scores)
        reg = sum(s["register"] for s in char_scores) / n
        unc = sum(s["uncertainty"] for s in char_scores) / n
        ch = sum(s["channel_coverage"] for s in char_scores) / n
        sep = sum(s["separation"] for s in char_scores) / n
        print(f"{char_id:<25} {reg:>8.2f} {unc:>8.2f} {ch:>8.2f} {sep:>8.2f} {n:>4}")

    # Aggregate
    n = len(all_scores)
    agg_reg = sum(s["register"] for s in all_scores) / n
    agg_unc = sum(s["uncertainty"] for s in all_scores) / n
    agg_ch = sum(s["channel_coverage"] for s in all_scores) / n
    agg_sep = sum(s["separation"] for s in all_scores) / n
    print(f"{'-'*80}")
    print(f"{'AGGREGATE':<25} {agg_reg:>8.2f} {agg_unc:>8.2f} {agg_ch:>8.2f} {agg_sep:>8.2f} {n:>4}")

    # Write results
    output_path = os.path.join(EVAL_DIR, args.transcript_dir.rstrip("/").split("/")[-1] + "-thinking-field-scores.json")
    results = {
        "transcript": args.transcript_dir,
        "character_filter": args.character,
        "events_scored": n,
        "aggregate": {
            "register": agg_reg,
            "uncertainty": agg_unc,
            "channel_coverage": agg_ch,
            "separation": agg_sep,
        },
        "per_character": {
            char_id: {
                "register": sum(s["register"] for s in cs) / len(cs),
                "uncertainty": sum(s["uncertainty"] for s in cs) / len(cs),
                "channel_coverage": sum(s["channel_coverage"] for s in cs) / len(cs),
                "separation": sum(s["separation"] for s in cs) / len(cs),
                "n": len(cs),
            }
            for char_id, cs in scores_by_char.items()
        },
        "detail": all_scores,
    }
    with open(output_path, "w") as f:
        json.dump(results, f, indent=2)
    print(f"\nResults written to {output_path}")


if __name__ == "__main__":
    main()
