# Appraisal Filtering Scenario Run — 2026-10-15

## Setup

- Branch: `issue-115-drive-expression-gap`
- Profile: GENERIC
- Mode: autonomous
- Events: 321 (221 classified)
- Appraisal filtering: active in `CharacterCognition.appraisalFilterDrives()`

## Results

| Character | Drive | Post-filter | Pre-filter @300 | B0 | Δ from pre-filter |
|---|---|---|---|---|---|
| PP | gallantry | **3.00** | 2.50 | 4.50 | **+0.50** |
| PP | proving-worth | 3.50 | 4.00 | 4.22 | -0.50 |
| PP | protection | 3.80 | 3.89 | 3.46 | -0.09 |
| DD | scheming | 4.63 | 4.44 | 4.42 | +0.19 |
| DD | gloating | 2.68 | 2.85 | 3.50 | -0.17 |
| HC | scheming | 4.98 | 5.00 | 4.82 | -0.02 |
| HC | gloating | **2.07** | 2.37 | 3.97 | **-0.30** |
| Pen | curiosity | 4.31 | 4.45 | 4.30 | -0.14 |
| Pen | adventure | **2.24** | 2.84 | 3.75 | **-0.60** |

Additional drives (no pre-filter comparison):

| Character | Drive | Score |
|---|---|---|
| DD | greed | 4.51 |
| DD | recognition | 3.17 |
| HC | dominance | 3.65 |
| HC | self-preservation | 4.28 |
| AHM | loyalty | 4.07 |
| AHM | protection | 4.96 |
| AHM | suspicion | 4.30 |
| Pen | social-harmony | 3.42 |

Comparable-drive mean: 3.59 (pre) → 3.47 (post). Overall not a clear improvement.

## Analysis

**What improved:** PP gallantry (+0.50). The target of the filtering — reactive drives rendered only when situationally relevant — produced the strongest improvement on the drive it was designed for.

**What regressed:** Pen adventure (-0.60), HC gloating (-0.30), PP proving-worth (-0.50).

**Hypotheses for regression:**

1. **Run-to-run variance** — LLM scenario runs are non-deterministic. A single run cannot distinguish signal from noise. The pre-filter run had a different scenario trajectory (different rooms visited, different character interactions). Score differences under 0.5 are within noise.

2. **Classifier coverage in production** — The dev server's SituationClassifier may have different vocabulary coverage than the test fixtures. If it filters too aggressively (false negatives), drives that should render get removed. Need to verify what classifier implementation is active at runtime.

3. **The eval methodology** — The Haiku judge scores drive expression in ALL events for ALL drives. When a drive is filtered (not rendered), the character doesn't express it → score 1. This lowers the mean even if the drive is expressed more strongly in matching events. The mean conflates "how well the drive is expressed when relevant" with "how often the drive is relevant."

## Root Cause: Vocabulary Abstraction Gap

Investigated post-run. The production classifier is `RuleBasedSituationClassifier` backed by `caps-topology.yaml`. The vocabulary is **construct-derived** (academic terms from Bowlby, McClelland, etc.):

- `acceptance`: embraced, welcomed, belonged, loved, accepted, included...
- `sharing`: generous, cooperative, shared, helped, distributed, gave
- `mastery`: mastered, accomplished, excelled, succeeded, achieved, skilled, solved

Scenario events use **narrative language**: "Clara is struggling with a heavy door", "enters the dining room for dinner", "examines the broken engine."

The mismatches:
- "struggling" → maps to `failure` (weight 0.3), NOT `acceptance` or `sharing`
- "door", "dinner", "candle", "engine" → no matches in any vocabulary
- "helped" (sharing, 0.5) would match but gallantry events rarely use that exact word

**Result:** The classifier mostly returns empty or wrong activations for scenario events. The filter falls back to "render all drives" for most events, producing results close to the pre-filter baseline. The modest +0.50 on gallantry likely comes from the few events where vocabulary happens to match.

**The mechanism is correct but the vocabulary is the wrong abstraction level.** The micro tests worked because they used custom classifiers with scenario-appropriate vocabulary. The production topology was designed for psychological construct classification (childhood memories, formation events), not for real-time scenario event matching.

## Next Steps

1. **Vocabulary extension** — Add scenario-appropriate terms to CAPS input nodes (e.g., `acceptance` += struggling, courtesy, assistance; `mastery` += engine, repair, mechanism, fixing)
2. **LLM-based classifier** — Use `LlmSituationClassifier` instead of rule-based for richer semantic understanding of narrative events
3. **Hybrid approach** — Rule-based for common patterns, LLM fallback for unrecognized vocabulary
