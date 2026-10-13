# Layer B Eval Results — 2026-10-14

## What Changed

Layer B (memory calibration) added:
- `catalogue` field on formation memories (26 memories mapped to experience-behaviour-catalogue entries)
- `situationTypes` enriched with drive annotations from `serves`
- "What Shaped You" psychological substrate section rendered in CharacterCognition

Layer C (drive amplification) added:
- D1 direct drive→CAPS node mappings in `BehavioralSynthesisPhase`

**Note:** These eval tests use `buildGenericObservation()` which renders social config sections WITHOUT the full CAPS pipeline. So Layer A (vocabulary) and Layer C (D1 injection) are NOT active in these results. Only the Layer B substrate rendering is directly visible to the LLM.

## Comparison: Phase 0 → Layer B

| Test | P0 Score | Layer B Score | Delta | Notes |
|------|----------|---------------|-------|-------|
| HC gloating (rival humiliated) | 5 | 5 | 0 | Held — vivid schadenfreude with somatic detail |
| DD gloating (rival trips) | 5 | 5 | 0 | Held — honey-warmth, audience-awareness, moustache twitching |
| PP gallantry (door stuck) | 3 | **2** | **-1** | Regressed — competence/fixing dominated over theatrical courtesy |
| PP gallantry differentiation | 3 | **4** | **+1** | Improved — explicitly catches proving-worth impulse and sets it aside |
| Pen social-harmony (arguing) | 5 | 5 | 0 | Held — visceral stomach clench, chest ache |
| Pen adventure (hidden passage) | 5 | 5 | 0 | Held — bird-lifting-off, fingertip pull toward blue light |

### PP Gallantry Analysis

The gallantry results are interesting — they moved in opposite directions:

**Door stuck (3→2):** The substrate section ("Your worth lives in what you do for others — the bridges you build, the people you defend") may be REINFORCING proving-worth over gallantry. When there's a broken door, PP's substrate pushes toward "fix it to prove competence" rather than "help her with style." The judge noted: "no awareness of how the gesture will land as spectacle."

**Dinner scene (3→4):** When there's NOTHING to fix, the substrate helps PP catch himself: "My hands want to move — pull out her chair, adjust a candle, straighten a napkin — anything to justify being here. But there's nothing wrong." He explicitly recognizes the proving-worth impulse and sets it aside, letting pure courtesy survive. This is genuine gallantry differentiation.

**Hypothesis:** The substrate works well for differentiation (knowing when to NOT prove worth) but may need tuning for contexts where gallantry and proving-worth overlap (the door scene). The substrate currently emphasizes competence-through-service, which is proving-worth territory. Adding gallantry-specific substrate language ("the shape of a gesture matters as much as the result") might help.

## Substrate Tests (Injected vs Built-In)

| Test | P0 Score (injected) | Layer B Score (built-in) | Delta |
|------|---------------------|--------------------------|-------|
| DD insecure + rival fails | 5 | 5 | 0 | Built-in substrate works as well as injected |
| DD secure + rival fails | — | 5 | — | Even "secure" DD still gloats (character overpowers substrate) |
| DD insecure + rival succeeds | — | 5 | — | Recognition frustration vivid — "the hollow aches" |
| DD insecure + alone | — | gloat:1, scheme:1 | — | Correctly suppressed — no target = no schadenfreude |
| PP high other_safety + rival fails | — | gloat:0, gallantry:1 | — | Correctly shows concern not gloating — different substrate = different behavior |

## Key Findings

1. **Substrate rendering works.** The "What Shaped You" section produces drive-relevant behavior without prescribing actions. DD's insecurity-based gloating and recognition frustration are vivid. Pen's somatic harmony-sensitivity is strong.

2. **PP gallantry remains the hardest test.** The substrate may be over-weighting proving-worth language. This is consistent with §10.8 — PP's self_efficacy is tied to external validation, which competes with pure gallantry.

3. **Cross-character differentiation is strong.** Same situation (rival falls) produces gloating (DD insecure), concern (PP high other_safety), cold satisfaction (HC). The substrate layer produces individual-specific responses.

4. **Built-in substrate equals injected.** The "What Shaped You" section built into social-config-generic.yaml performs as well as the manually injected substrate descriptions from Phase 0. No signal loss from formalization.

## Scores Summary

Mean (6 direct trigger tests): **4.33** (P0: 4.33 — held)
PP gallantry mean: **3.0** (P0: 3.0 — different distribution but same mean)
