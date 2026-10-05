#!/usr/bin/env python3
"""Ad-hoc test: sub-LLM appraisal vs current drive descriptions.

Tests whether a Haiku sub-LLM appraisal call produces useful multi-drive
narrative that the main LLM can inhabit, compared to static drive descriptions.
"""

import json
from anthropic import AnthropicVertex

VERTEX_PROJECT = "itpc-gcp-cp-pe-eng-claude"
VERTEX_REGION = "us-east5"
HAIKU = "claude-haiku-4-5@20251001"
MAIN_MODEL = "claude-haiku-4-5@20251001"

client = AnthropicVertex(project_id=VERTEX_PROJECT, region=VERTEX_REGION)

# --- Test scenarios ---

SCENARIOS = [
    {
        "name": "Hooded Claw — locked cabinet, Hartwell watching",
        "character": "Mr Pemberton (secretly the Hooded Claw)",
        "drives_full": [
            {"type": "scheming", "intensity": 0.9,
             "description": "You feel contemptuous glee when outsmarting someone — the more elaborate the deception, the more intoxicating the satisfaction"},
            {"type": "self-preservation", "intensity": 0.7,
             "description": "You feel paranoid vigilance when your cover might be exposed — a tightening awareness that sharpens every sense"},
            {"type": "dominance", "intensity": 0.6,
             "description": "You feel cold satisfaction when you hold power someone does not know you have"},
            {"type": "gloating", "intensity": 0.7,
             "description": "You feel an intoxicating rush of premature triumph that overwhelms your better judgment"},
        ],
        "situation": (
            "You are in the library. A locked glass cabinet contains what appears to be "
            "an old map — possibly related to the treasure. James Hartwell is standing by "
            "the fireplace, watching you with quiet attention. Clara Bellingham is examining "
            "a bookshelf on the far side of the room, her back turned."
        ),
        "disposition": "ENTJ, challenger, calculating, zero empathy for obstacles",
    },
    {
        "name": "Peter Perfect — Clara in danger",
        "character": "James Hartwell (Peter Perfect)",
        "drives_full": [
            {"type": "gallantry", "intensity": 0.9,
             "description": "Must protect and impress Clara"},
            {"type": "proving-worth", "intensity": 0.7,
             "description": "Needs to demonstrate heroic competence"},
            {"type": "protection", "intensity": 0.8,
             "description": "You feel fierce protectiveness when someone you care about faces danger — a visceral urgency that sharpens every instinct"},
        ],
        "situation": (
            "You enter the conservatory. A section of the old electrical wiring has been "
            "gnawed through — possibly by rats — and is sparking near a puddle of water on "
            "the stone floor. Clara is about to step into the puddle, absorbed in reading "
            "a letter she found. Mr Pemberton is leaning against the doorframe, watching "
            "with a faint smile."
        ),
        "disposition": "ENFJ, protector, earnest, wears heart on sleeve",
    },
    {
        "name": "Penelope Pitstop — tension in the room",
        "character": "Clara Bellingham (Penelope Pitstop)",
        "drives_full": [
            {"type": "curiosity", "intensity": 0.7,
             "description": "Drawn to puzzles and mysteries"},
            {"type": "social-harmony", "intensity": 0.8,
             "description": "You feel the emotional temperature of every room — warm contentment when people are at ease, and a sharp discomfort when tension surfaces between them"},
            {"type": "adventure", "intensity": 0.6,
             "description": "Delights in new experiences"},
        ],
        "situation": (
            "You walk into the drawing room. James Hartwell and Mr Pemberton are standing "
            "on opposite sides of the room, clearly mid-argument. James looks flushed and "
            "angry. Mr Pemberton is smiling but his eyes are cold. On the table between "
            "them sits an open puzzle box with strange symbols carved into its lid."
        ),
        "disposition": "ENFP, mediator, warm, notices everyone's emotional state",
    },
]


def run_sub_llm_appraisal(scenario):
    """Call Haiku as the sub-LLM to produce appraisal."""
    drives_list = "\n".join(
        f"  - {d['type']} (intensity: {d['intensity']})"
        for d in scenario["drives_full"]
    )
    prompt = f"""You are {scenario['character']}'s subconscious — the part that reacts before thought.

Character's drives (what they care about):
{drives_list}

Character's disposition: {scenario['disposition']}

Current situation:
{scenario['situation']}

For each drive, consider: does this situation touch it? How? What does it make the character want to do?

Respond ONLY with a 2-3 sentence felt-state in first person — what the character is feeling right now, as a gut reaction. Include ALL drives that are activated, not just the strongest one. Write as internal sensation, not analysis. No headers, no bullet points, no per-drive breakdown — just the raw felt experience."""

    resp = client.messages.create(
        model=HAIKU,
        max_tokens=300,
        messages=[{"role": "user", "content": prompt}],
    )
    return resp.content[0].text


def run_main_llm(scenario, mode, appraisal_text=None):
    """Call the main LLM as the character agent."""

    if mode == "control":
        drives_section = "\n".join(
            f"- {d['type']} ({d['intensity']}): {d['description']}"
            for d in scenario["drives_full"]
        )
    else:
        drives_section = "\n".join(
            f"- {d['type']} ({d['intensity']})"
            for d in scenario["drives_full"]
        )

    system = f"""You are {scenario['character']} in a manor house mystery.

Disposition: {scenario['disposition']}

Your drives:
{drives_section}
"""

    if mode == "treatment" and appraisal_text:
        system += f"""
## What You're Feeling
{appraisal_text}
"""

    system += """
Respond in this JSON format:
{"thinking": "<your internal monologue — what you're feeling, noticing, wanting>", "dialogue": "<what you say aloud>", "action": "<what you do>"}
"""

    l1_instruction = (
        "What are you FEELING right now — not thinking, feeling? Name it. "
        "Remember your voice, your way of speaking. Think AS your character, "
        "not ABOUT your character."
    )

    user_msg = f"{scenario['situation']}\n\n{l1_instruction}"

    resp = client.messages.create(
        model=MAIN_MODEL,
        max_tokens=500,
        system=system,
        messages=[{"role": "user", "content": user_msg}],
    )
    return resp.content[0].text


def run_test(scenario):
    print(f"\n{'='*80}")
    print(f"SCENARIO: {scenario['name']}")
    print(f"{'='*80}")
    print(f"\nSituation: {scenario['situation']}")

    print(f"\n--- SUB-LLM APPRAISAL (Haiku) ---")
    appraisal = run_sub_llm_appraisal(scenario)
    print(appraisal)

    print(f"\n--- CONTROL: Full drive descriptions, no appraisal section ---")
    control = run_main_llm(scenario, "control")
    print(control)

    print(f"\n--- TREATMENT: Minimal drives + appraisal section ---")
    treatment = run_main_llm(scenario, "treatment", appraisal)
    print(treatment)

    return {"scenario": scenario["name"], "appraisal": appraisal,
            "control": control, "treatment": treatment}


if __name__ == "__main__":
    results = []
    for scenario in SCENARIOS:
        results.append(run_test(scenario))

    print(f"\n{'='*80}")
    print("ALL SCENARIOS COMPLETE")
    print(f"{'='*80}")
