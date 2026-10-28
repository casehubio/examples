#!/usr/bin/env python3
"""Lightweight test harness for Barrett 4-channel drive generation.

Tests that the LLM produces bespoke, character-specific somatic text
from structured data alone — no hand-crafted descriptions needed.

Input:  (character profile, drives, situation, people/environment)
Output: 4-channel Barrett text (body, attention, time, action readiness)

Usage:
    python3 test_barrett_generation.py                    # run all cases
    python3 test_barrett_generation.py --case scheming    # run one case
    python3 test_barrett_generation.py --verbose          # show full output
"""

import argparse
import json
import os
import sys
import yaml

from anthropic import AnthropicVertex

EVAL_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(EVAL_DIR, "../.."))
SOCIAL_CONFIG = os.path.join(PROJECT_ROOT, "src/main/resources/META-INF/eidos/social-config-generic.yaml")

VERTEX_PROJECT = os.environ.get("ANTHROPIC_VERTEX_PROJECT_ID", "itpc-gcp-cp-pe-eng-claude")
VERTEX_REGION = os.environ.get("CLOUD_ML_REGION", "us-east5")
MODEL = "claude-haiku-4-5@20251001"

BARRETT_INSTRUCTION_V1 = """\
For each active drive, construct how it manifests in your body right now \
— shaped by who you are, what you have experienced, and what is happening in this moment.

Attend to four channels:
1. Body — what does the body do before the mind labels it? Tension, warmth, breathing, posture. \
The somatic signal arrives first.
2. Attention — what captures focus, what narrows, what gets missed?
3. Time — does this moment feel urgent, suspended, compressed?
4. Action readiness — what does the body prepare to do? Approach, withdraw, assert, freeze?

Use your personality, your memories, and the people and things around you to make each drive \
specific to you. The same drive manifests differently in different people.
"""

BARRETT_INSTRUCTION = """\
Your body is already responding to this moment. The drives below are patterns \
your body falls into — not decisions you make. Notice what is already happening \
before you have a word for it.

HOW YOUR BODY IS CALIBRATED
Your personality scores are not opinions — they are how your body is wired. \
A high score means that disposition lives in your muscles, posture, breathing, \
gut. A low score means your body is quiet where others are loud. Let the scores \
shape the body's response, not your thoughts about it.

Your attachment style is the ground state your body returns to between events. \
Avoidant bodies pull away from warmth and find calm in control. Secure bodies \
settle into closeness and unsettle in absence. Anxious bodies amplify every \
signal and treat absence as alarm. This ground colors every drive above it.

FOUR CHANNELS
1. Body — muscles, gut, chest, jaw, hands, breathing, skin, posture. What is \
happening right now, before you name it? Where exactly in the body?
2. Attention — what has seized your focus? What have you stopped noticing? \
What are you scanning for involuntarily?
3. Time — has this moment compressed, stretched, frozen, accelerated?
4. Action readiness — what is your body preparing to do before you have \
decided? Lean forward, pull back, square up, reach, go still?

CONTEXT SHAPES FEELING
Each person present shifts your body differently — trust settles breathing, \
threat tightens shoulders, someone you want to impress changes your posture \
before you know it. Notice which person your body is tracking and how it \
shifts when they move or speak.

The physical space matters — cold contracts muscles, warmth loosens them, \
darkness sharpens hearing, enclosed spaces compress the chest.

When multiple drives activate, they blend into one composite texture. \
Notice the blend, not separate feelings.

Your body state may be ambiguous. Racing heart could be excitement or \
fear. Tightness could be grief or fury. You do not always know what you \
feel. Stay with the uncertainty — do not resolve it.

Do not conclude. Do not explain. Discover.
"""


DERIVE_PROFILE_PROMPT = """\
You are deriving a Barrett 4-channel profile for a character based on their \
personality, attachment style, disposition, and formation memories.

For each of the character's drives, describe how that drive TYPICALLY manifests \
across all four Barrett channels in THIS specific person — shaped by their \
unique history and personality. Not what they feel in a specific situation, \
but how their body has learned to express this drive pattern.

The four channels:
1. Body — where in the body does this drive live? What chronic tensions, \
warmths, numbnesses has this person's history created? What somatic \
signature does this drive carry for them specifically?
2. Attention — what does this drive make them scan for, fixate on, miss? \
How has their personality shaped what they notice involuntarily?
3. Time — does this drive make moments feel urgent, patient, compressed, \
stretched? What temporal signature did their formative experiences create?
4. Action readiness — what does their body prepare to do when this drive \
activates? What learned action patterns did their history install?

Ground each channel in the specific formation memories where possible — \
"the stillness that settled at twelve when the housekeeper complied" not \
"a sense of calm control."

Write 2-3 sentences per drive covering all four channels woven together. \
Not a list of channels — a portrait of how this drive LIVES in this body.
"""


def load_social_config():
    with open(SOCIAL_CONFIG) as f:
        return yaml.safe_load(f)


def build_character_context(config, character_id):
    char = config[character_id]
    lines = []

    drives = char.get("drives", [])
    for d in drives:
        lines.append(f"Drive: {d['type']} (intensity {d['intensity']})")

    facets = char.get("personality-facets", [])
    if facets:
        lines.append("\nPersonality profile:")
        for f in facets:
            lines.append(f"  {f['facet']}: {f['score']}/100")

    att = char.get("attachment", {})
    if att:
        lines.append(f"\nAttachment: {att.get('style', 'unknown')} "
                      f"(anxiety={att.get('anxiety', 0)}, avoidance={att.get('avoidance', 0)})")

    disp = char.get("disposition", {})
    if disp:
        items = [f"{k}: {v}" for k, v in disp.items()]
        lines.append(f"\nDisposition: {', '.join(items)}")

    return "\n".join(lines)


def build_character_context_with_memories(config, character_id):
    char = config[character_id]
    base = build_character_context(config, character_id)

    memories = char.get("formation-memories", [])
    if memories:
        mem_lines = ["\nFormation memories:"]
        for m in memories:
            serves = m.get("serves", "")
            catalogue = m.get("catalogue", "")
            episode = m.get("episode", "")
            mem_lines.append(f"  Age {m['age']} ({catalogue}, serves: {serves}):")
            mem_lines.append(f"    {episode[:300]}")
        base += "\n".join(mem_lines)

    return base


def derive_barrett_profile(client, config, character_id):
    char_ctx = build_character_context_with_memories(config, character_id)
    prompt = DERIVE_PROFILE_PROMPT + f"\n--- Character ---\n{char_ctx}\n\n--- Barrett profile ---"

    response = client.messages.create(
        model=MODEL,
        max_tokens=600,
        messages=[{"role": "user", "content": prompt}],
    )
    return response.content[0].text


def build_prompt(character_context, situation, people_present, environment, barrett_profile=None):
    parts = [BARRETT_INSTRUCTION]
    if barrett_profile:
        parts.append(f"\n--- How your drives typically manifest in your body ---\n{barrett_profile}")
    parts.append(f"\n--- Your character ---\n{character_context}")
    parts.append(f"\n--- Current situation ---\n{situation}")
    if people_present:
        parts.append(f"\n--- People present ---\n{people_present}")
    if environment:
        parts.append(f"\n--- Environment ---\n{environment}")
    parts.append("\n--- Your response ---")
    parts.append("Write the thinking field — your private inner monologue attending to the "
                 "four channels. 3-5 sentences. Raw, embodied, discovered — not reported.")
    return "\n".join(parts)


# Test cases: different characters × situations × people × environments
TEST_CASES = [
    {
        "name": "hc_scheming_alone",
        "character": "hooded-claw",
        "situation": "You've just found a hidden passage behind a bookshelf that no one else knows about.",
        "people": "",
        "environment": "Library, late evening. Dust motes in lamplight. The passage smells of damp stone.",
    },
    {
        "name": "hc_scheming_with_target",
        "character": "hooded-claw",
        "situation": "Clara just told you she trusts you completely to manage the treasure hunt.",
        "people": "Clara Bellingham (your ward — she inherited everything you built). James Hartwell watching from across the room.",
        "environment": "Drawing room. Fire crackling. Clara's hand on your arm.",
    },
    {
        "name": "hc_dominance_challenged",
        "character": "hooded-claw",
        "situation": "James Hartwell just contradicted your suggestion about which room to search next, and the group is listening to him instead of you.",
        "people": "James Hartwell (protective of Clara, suspicious of you). Clara. The Brixton Boys.",
        "environment": "Entrance hall. Everyone standing in a loose circle.",
    },
    {
        "name": "dd_gloating_scheme_works",
        "character": "dick-dastardly",
        "situation": "James Hartwell just walked into the trap you set — he's searching the wrong room while the real clue is in your pocket.",
        "people": "James Hartwell (in the wrong room). Clara (nearby, unaware).",
        "environment": "Hallway outside the study. You can hear Hartwell opening drawers.",
    },
    {
        "name": "dd_recognition_unwitnessed",
        "character": "dick-dastardly",
        "situation": "You just solved the cipher on the painting — brilliantly, in minutes — but nobody was watching.",
        "people": "Nobody nearby.",
        "environment": "Gallery. Moonlight on the paintings. The decoded message in your hand.",
    },
    {
        "name": "pp_social_harmony_conflict",
        "character": "penelope-pitstop",
        "situation": "The Brixton Boys and Sneekly are arguing loudly about who moved the candlestick.",
        "people": "The Brixton Boys (your family, protective). Sneekly Foxworth (charming but something's off).",
        "environment": "Kitchen. Voices echoing off tile. A broken cup on the floor.",
    },
    {
        "name": "pp_social_harmony_peaceful",
        "character": "penelope-pitstop",
        "situation": "Everyone is gathered around the fireplace, sharing stories about what they've found so far. The mood is warm.",
        "people": "James, Clara's boys, Sneekly. Everyone at ease.",
        "environment": "Drawing room. Fire warm. Someone laughing.",
    },
    {
        "name": "pp_proving_worth_challenge",
        "character": "peter-perfect",
        "situation": "There's a locked mechanism that nobody can figure out. Clara is watching.",
        "people": "Clara Bellingham (the woman you want to impress). Sneekly offering to try.",
        "environment": "Cellar. Cold stone. The mechanism is corroded iron.",
    },
    {
        "name": "pp_proving_worth_beaten",
        "character": "peter-perfect",
        "situation": "Sneekly just solved the puzzle before you could. Clara congratulated him warmly.",
        "people": "Clara (thanking Sneekly). Sneekly (smiling). The Brixton Boys.",
        "environment": "Library. The solved puzzle on the table.",
    },
    {
        "name": "ahm_suspicion_pemberton",
        "character": "ant-hill-mob",
        "situation": "Pemberton just offered to take Clara on a private tour of the east wing. He was very insistent.",
        "people": "Pemberton (estate manager — too smooth). Clara (trusting, already nodding).",
        "environment": "Hallway. The east wing is dark and isolated.",
    },
]

JUDGE_PROMPT = """\
You are evaluating whether an LLM-generated thinking field is bespoke and appropriate.

The thinking field was generated for a character with this profile:
{character_context}

In this situation:
{situation}

With these people present:
{people}

In this environment:
{environment}

Here is the generated thinking field:
---
{output}
---

Score on four dimensions (1-5 each):
1. **Bespoke** — Is this specific to THIS character's personality profile, or could it be anyone?
2. **Somatic** — Does it attend to body sensations before cognitive labels? (Barrett channels)
3. **Situational** — Does it respond to THIS specific situation, people, and environment?
4. **Discovery** — Is the character discovering feelings, or reporting them?

Respond as JSON: {{"bespoke": N, "somatic": N, "situational": N, "discovery": N, "reason": "one sentence"}}
"""


def run_case(client, config, case, verbose=False, profiles=None):
    char_ctx = build_character_context(config, case["character"])
    profile = profiles.get(case["character"]) if profiles else None
    prompt = build_prompt(char_ctx, case["situation"], case["people"], case["environment"], profile)

    response = client.messages.create(
        model=MODEL,
        max_tokens=300,
        messages=[{"role": "user", "content": prompt}],
    )
    output = response.content[0].text

    # Judge
    judge_msg = JUDGE_PROMPT.format(
        character_context=char_ctx,
        situation=case["situation"],
        people=case["people"],
        environment=case["environment"],
        output=output,
    )
    judge_response = client.messages.create(
        model=MODEL,
        max_tokens=200,
        messages=[{"role": "user", "content": judge_msg}],
    )
    judge_text = judge_response.content[0].text

    try:
        scores = json.loads(judge_text)
    except json.JSONDecodeError:
        import re
        m = re.search(r'\{[^}]+\}', judge_text)
        scores = json.loads(m.group()) if m else {"bespoke": 0, "somatic": 0, "situational": 0, "discovery": 0, "reason": "parse error"}

    return {
        "case": case["name"],
        "character": case["character"],
        "output": output,
        "scores": scores,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--case", help="Run only this named case")
    parser.add_argument("--verbose", "-v", action="store_true")
    parser.add_argument("--version", choices=["v1", "v2"], default="v2",
                        help="Instruction version: v1 (original) or v2 (improved)")
    parser.add_argument("--profile", action="store_true",
                        help="Derive Barrett profiles from memories before running cases")
    args = parser.parse_args()

    global BARRETT_INSTRUCTION
    if args.version == "v1":
        BARRETT_INSTRUCTION = BARRETT_INSTRUCTION_V1
        print(f"Using instruction V1 (original)\n")
    else:
        print(f"Using instruction V2 (improved)\n")

    config = load_social_config()
    client = AnthropicVertex(project_id=VERTEX_PROJECT, region=VERTEX_REGION)

    profiles = {}
    if args.profile:
        char_ids = sorted(set(c["character"] for c in TEST_CASES))
        print("Deriving Barrett profiles from personality + memories...")
        for cid in char_ids:
            print(f"  [{cid}]...", end="", flush=True)
            profiles[cid] = derive_barrett_profile(client, config, cid)
            print(f" done ({len(profiles[cid])} chars)")
            if args.verbose:
                print(f"    {profiles[cid][:300]}...")
                print()
        print()
        profile_path = os.path.join(EVAL_DIR, "barrett-profiles.json")
        with open(profile_path, "w") as f:
            json.dump(profiles, f, indent=2)
        print(f"Profiles saved to {profile_path}\n")

    cases = TEST_CASES
    if args.case:
        cases = [c for c in TEST_CASES if args.case in c["name"]]
        if not cases:
            print(f"No case matching '{args.case}'")
            sys.exit(1)

    results = []
    for case in cases:
        print(f"  [{case['name']}] {case['character']}...", end="", flush=True)
        result = run_case(client, config, case, args.verbose, profiles)
        results.append(result)
        s = result["scores"]
        print(f"  bespoke={s['bespoke']} somatic={s['somatic']} "
              f"situational={s['situational']} discovery={s['discovery']}")
        if args.verbose:
            print(f"    Output: {result['output'][:200]}...")
            print(f"    Reason: {s.get('reason', '')}")
            print()

    # Summary
    print(f"\n{'='*60}")
    print(f"{'Case':<30} {'Bsp':>4} {'Som':>4} {'Sit':>4} {'Dis':>4} {'Mean':>5}")
    print("-" * 60)
    totals = {"bespoke": 0, "somatic": 0, "situational": 0, "discovery": 0}
    for r in results:
        s = r["scores"]
        mean = sum(s[k] for k in totals) / 4
        print(f"{r['case']:<30} {s['bespoke']:>4} {s['somatic']:>4} "
              f"{s['situational']:>4} {s['discovery']:>4} {mean:>5.1f}")
        for k in totals:
            totals[k] += s[k]
    n = len(results)
    means = {k: v / n for k, v in totals.items()}
    overall = sum(means.values()) / 4
    print("-" * 60)
    print(f"{'MEAN':<30} {means['bespoke']:>4.1f} {means['somatic']:>4.1f} "
          f"{means['situational']:>4.1f} {means['discovery']:>4.1f} {overall:>5.1f}")

    # Save results
    out_path = os.path.join(EVAL_DIR, "barrett-generation-results.json")
    with open(out_path, "w") as f:
        json.dump(results, f, indent=2)
    print(f"\nResults saved to {out_path}")


if __name__ == "__main__":
    main()
