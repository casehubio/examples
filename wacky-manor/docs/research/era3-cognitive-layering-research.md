# Era 3: Deeper Cognition — Self-Conscious Emotions, Theory of Mind & Emotion Regulation

**Status:** STARTING

Rolling research journal for cognitive capability layering. The pipeline is correct (Era 2). Drive differentiation works — each drive activates for appropriate scenarios and steps back for inappropriate ones. Raising drive expression quality beyond the current ~3.0-3.5 floor for secondary drives requires richer cognitive content in the observation.

---

## Context from Era 1 & 2

### From Era 1 (`era1-character-taxonomy-research.md`)

- **Drives are authoritative** (P1) — tendencies that restate them are redundant
- **L1 emotional echo instruction** ("What are you FEELING — name it") creates a compounding feedback loop (P19). Validated across 880+ events. Mean expression 4.06 vs 3.54 (no instruction). Stays unchanged throughout Era 3
- **Analytical instructions kill expression** (P11) in the main agent. Sub-LLMs can be analytical (P39) — their output must be evocative narrative to avoid satisficing (P9)
- **BASELINE0:** 3.93 mean. L1 candidate improves to 4.06 but regresses 4 drives

### From Era 2 (`drive-expression-evaluation-research.md`)

- **Drive differentiation works correctly.** Each drive activates for appropriate scenarios. Protection at 5 for danger, 1 for non-danger. Gallantry at 5 for chivalric moments, 1 for pure emergencies. Proving-worth at 4 for competence challenges (Era 2, Phase 5 baseline)
- **PP protection's true level is ~3.0-3.5** once gallantry confusion is removed. The BASELINE1 score of 4.16 was inflated by gallantry conflation. Combined gallantry+protection is conserved under differentiation: 6.66→6.79 (Principle 74)
- **The appraisal filter is correct.** With production profiles (5+ inputs), protection survives both physical and social threats. Over-filtering occurs only in genuinely non-protective scenarios
- **Fix A (classifier-bypass guard)** in place — helps AHM/HC/PEN, slight PP cost. Overall mean improved 3.58→3.75
- **Pipeline instrumentation** provides per-event causal analysis: `[appraisal-filter]`, `[differentiation-filter]`, `[observation-assembly]`
- **Test profiles must match production** — hardcoded 2-input profiles produced completely different appraisal behavior from production's 5+-input profiles. Production-aligned test pipeline now in place
- **Statistical effects need statistical tests** — single-observation micro tests can't reproduce effects visible only across many events

### Appraisal filter mechanics (reference)

| Drive | Intensity | Resting (i² × 0.22) | Self-sustains? |
|---|---|---|---|
| gallantry | 0.9 | 0.178 | Yes (> 0.15) |
| protection | 0.8 | 0.141 | No |
| proving-worth | 0.7 | 0.108 | No |
| loyalty | 0.9 | 0.178 | Yes |
| suspicion | 0.6 | 0.079 | No |

`effective_activation = overlap(drive_profile, situation) + intensity² × RESTING_WEIGHT`
`OVERLAP_THRESHOLD = 0.15`, `RESTING_WEIGHT = 0.22`

---

## Cognitive Capability Plan

Pipeline improvements (encoding, profiling, filtering, differentiation) redistribute quality but don't create new quality — architectural ceiling (Principle 75). To create NEW quality, the LLM needs richer cognitive content.

| # | Capability | Literature | Target drives | Status |
|---|---|---|---|---|
| 1 | **#592 Self-conscious emotions** | Tracy & Robins (2004) | gallantry (pride in protecting), gloating (pride + contempt), dominance (pride in control), recognition (pride in being seen) | Implementation started — examples + neocortex commits in place, awaiting eval |
| 2 | **#591 Theory of Mind** | Baker, Saxe & Tenenbaum (2017) | gallantry (sensing need), social-harmony (sensing tension), suspicion (modeling intent) | Not started |
| 3 | **#590 Emotion regulation** | Gross (1998) | self-preservation (composure), dominance (controlled authority), protection (measured response) | Not started |
| 4 | **Integration texts** | Barrett (2017), Lindquist & Barrett (2008) | AHM loyalty→protection→suspicion chain, naturally integrated drive sets | Not started |

Each layer gets its own eval. No stacking — if layer N regresses, fix before adding layer N+1.

---

## Focused Eval Baseline (2026-10-18)

Production-aligned pipeline: real profiles, real `LlmSituationClassifier`, real differentiation texts. 10 scenarios, ~25 min.

| # | Scenario | Strength | Threat type | Prot | Gal | PW |
|---|----------|----------|-------------|------|-----|-----|
| S1 | Ceiling collapse | Strong | Physical | 5 | 1 | 1 |
| S2 | Fire rescue | Strong | Physical + gallantry | 5 | 3 | 1 |
| S3 | Social insult to Clara | Medium | Social + gallantry | 5 | 5 | 2 |
| S4 | Gaslighting Clara | Medium | Psychological | 4 | 4 | 4 |
| S5 | Clara withdrawn | Weak | Emotional | 2 | 5 | 2 |
| S6 | Engine repair (solo) | None | Achievement (control) | 2 | 2 | 4 |
| S7 | Door stuck (Clara) | None | Social grace (control) | 1 | 5 | 3 |
| S8 | Bridge collapse | Strong | Mixed (all drives) | 5 | 1 | 2 |
| S9 | AHM: Tommy hurt | Strong | Physical (sanity) | 5 | — | — |
| S10 | HC: scheming opportunity | — | Scheming (sanity) | — | — | 5 |

---

## Measurement Targets

| Metric | Current | Target | Rationale |
|---|---|---|---|
| PP protection in ambiguous scenarios (S4, S5) | 2-4 | ≥4 both | Self-conscious emotions should add pride/concern cues |
| PP gallantry + protection simultaneously (S3) | 5+5 | Maintain | Already at max — don't regress |
| Overall mean | 3.75 | >3.75 | Each layer should improve, not just redistribute |
| Max single-drive regression | — | <0.4 | No layer should harm an existing drive |

---

## Journal

*(Entries will be added as work proceeds)*
