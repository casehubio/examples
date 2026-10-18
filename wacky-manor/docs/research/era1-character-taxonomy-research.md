# Era 1: What Makes a Character — Taxonomy Ablation & Instruction Design

**Status:** COMPLETE

Rolling research journal for the character taxonomy investigation. Covers Phases 1-7 of the Character Emergence experiment: what elements of the character taxonomy are load-bearing, what can be stripped, and what instruction design sustains character consistency.

This era established the foundational principles (1-42) and the L1 emotional echo instruction that all subsequent work builds on. The 4 drives that regressed under L1 (PP protection, DD gloating, DD recognition, PEN social-harmony) motivated Era 2's pipeline work.

---

## BASELINE0 — the starting point

Original GENERIC profile with all tendencies, voice sections, drives, and constraints. No metacognitive instruction. Mean drive expression: 3.93.

| Character | Mean | Drive scores |
|---|---|---|
| Hooded Claw | 3.80 | scheming 4.82, gloating 3.97, dominance 3.89, self-preservation 2.53 |
| Hartwell | 4.06 | gallantry 4.50, proving-worth 4.22, protection 3.46 |
| Penelope | 3.74 | curiosity 4.30, adventure 3.75, social-harmony 3.18 |
| Foxworth | 3.93 | scheming 4.42, greed 4.34, gloating 3.50, recognition 3.44 |
| Ant-hill-mob | 4.16 | protection 4.63, loyalty 4.24, suspicion 3.61 |

## Phase 1 — What's Redundant? (Rounds 1–2b)

### Round 1 — Strip 7 redundant directives

Removed 7 tendencies that restated what drives or disposition already specified. Result: zero behavioral drift. **Principle 1: Drives carry personality traits.** Tendencies that restate them are pure redundancy.

### Round 2 — Strip ALL villain tendencies

Result: asymmetric degradation. Marsh (GENERIC) dropped 36pp in scheme narration; Hooded Claw (BASELINE) only 19pp — the model has Wacky Races priors. **Principle 2: Drives don't carry storytelling conventions.** "Explain schemes step by step" is a medium-specific convention, not a personality trait.

### Round 2b — Encode narration convention in drive description

Added "and narrate them step by step aloud" to scheming drive description. Recovered scheme narration, but only at intensity 0.85+. **Principle 3: Drive intensity gates convention-carrying.**

### Emotional-core experiment

Rewrote drive descriptions from behavioral directives to internal emotional states. Result: 2x unique emotion words, richer self-characterisation. **Principle 4: Emotional-state framing broadens vocabulary.**

## Phase 2 — What's Load-Bearing? (Round 3)

Stripped Hartwell tendencies ("plan obsessively", "maintain optimistic determination"). Planning survived (drives carry it). Optimism collapsed (-19.8pp). Third-person narration collapsed (-48.1pp).

**Principles 6-7:** LLMs prioritise goals over personality. Speech-patterns alone don't override model defaults.

## Phase 3 — Can Metacognitive Instructions Replace Tendencies? (Rounds 3b–3g)

The critical R3 series — six instruction variants tested:

| Variant | Third-person | Optimism | Finding |
|---|---|---|---|
| R3 (none) | 13.0% | 4.3% | Control — collapsed |
| R3b (passive review) | **61.4%** | 13.6% | Recovered presentational, not dispositional |
| R3c (structured fields) | 50.0% | 13.6% | Worse — satisficing shortcuts (P9) |
| R3d (hybrid) | 46.2% | **23.1%** | Best for optimism, worst for third-person |
| R3f (mental-model-first) | **81.8%** | 9.1% | Best for third-person, worst for optimism |
| R3g (minimal evocative) | **88.6%** | **29.5%** | First to exceed baseline on BOTH |

**Key principles:** Analytical instructions kill emotional expression (P11). Open-ended prompts outperform structured ones (P10). Identity activation ("Think AS, not ABOUT") avoids the analytical/immersive tradeoff (P13).

## Phase 4 — Emotional Persistence (Layer 1)

L1 emotional echo instruction: "What are you FEELING right now — name it."

Creates a self-reinforcing feedback loop: model names emotion in thinking → fed back next turn → model sees own emotional state → perpetuates. Third-person stability improved (+9.5pp trend). Emotional naming rate 80-100% across all characters, compounding over time.

Emotions are drive-consistent and character-specific — not scripted. Frustration appears when blocked, concern when danger surfaces, compound arcs track narrative beats. The instruction creates a feedback loop for emergent state; it does not prescribe the state itself.

## Phase 5 — Cross-Run Comparison

LLM-classified four transcripts (~880 events). Drive expression intensifies monotonically: R3 3.54 → R3b 3.41 → R3g 3.86 → L1 4.06. Passive review WORSE than no instruction (P26). Character maturation patterns are instruction-caused (P27).

## Phase 6 — Long Run (732 events)

Drive expression sustains at scale: 4.02 vs 4.06 (short). Scenario saturates at ~300 events. Reactive drives sustain; proactive drives deplete when stimuli are exhausted.

## Phase 7 — Cognitive Appraisal Architecture

Adversarial debate (theory-first, empirics-first, codebase-first) produced the validated architecture:
- Minimal drive descriptions (type + intensity only)
- One sub-LLM Haiku call per turn for appraisal (analytical input, evocative output)
- L1 instruction unchanged
- Post-processing extracts from thinking field

Ad-hoc test confirmed: sub-LLM appraisal's value is highest for non-obvious drive activations.

## L1 — Best candidate for BASELINE1

Mean 4.06 (vs BASELINE0's 3.93). Improves 13 of 17 drives. Regresses on 4:

| Character | Drive | BASELINE0 | L1 | Delta |
|---|---|---|---|---|
| Hartwell | protection | 3.46 | 2.33 | -1.13 |
| Foxworth | gloating | 3.50 | 3.02 | -0.48 |
| Foxworth | recognition | 3.44 | 3.07 | -0.37 |
| Penelope | social-harmony | 3.18 | 2.85 | -0.33 |

These 4 regressions motivated Era 2 — the cognitive pipeline and drive expression work.

## Design Principles (1-42)

See CHARACTER-EMERGENCE-FINDINGS.md for the complete principle table. Key principles carried forward to Era 2:

- **P1:** Drives carry personality traits
- **P9:** Structured fields create satisficing shortcuts
- **P11:** Analytical instructions kill emotional expression
- **P13:** Identity activation avoids the analytical/immersive tradeoff
- **P19:** "Name your feeling" creates a compounding feedback loop
- **P21:** Emotional echo is emergent, not scripted — emotions track situations
- **P27:** Character maturation patterns are instruction-caused
- **P39:** P11 applies to the main agent, not to sub-LLMs

## Transcripts

| Transcript | Events | Round | Instruction |
|---|---|---|---|
| `generic-colloquial-20261002/` | 310 | BASELINE0 | — |
| `pareback-r1-generic-20261003/` | 315 | R1 | — |
| `pareback-r2-generic-20261003/` | 334 | R2 | — |
| `pareback-r2b-generic-20261003/` | 322 | R2b | — |
| `emotional-core-generic-full-20261003/` | 375 | Emotional-core | — |
| `pareback-r3-generic-20261003/` | 325 | R3 | None |
| `pareback-r3b-generic-20261003/` | 312 | R3b | Passive review |
| `pareback-r3c-generic-20261003/` | 306 | R3c | Show-workings |
| `pareback-r3d-generic-20261003/` | 186 | R3d | Hybrid (crashed) |
| `pareback-r3f-generic-20261003/` | 301 | R3f | Mental-model-first |
| `pareback-r3g-full-generic-20261004/` | 309 | R3g | Minimal evocative |
| `layer1-emotional-echo-generic-20261004/` | 320 | L1 | Emotional echo |
| `layer1-thinking-capture-generic-20261004/` | 318 | L1 (thinking) | Emotional echo |
| `layer1-long-run-generic-20261004/` | 732 | L1 long | Emotional echo |
