# Phase 0 Eval Results — Two-Run Summary

**Date:** 2026-10-13
**Profile:** GENERIC
**Branch:** issue-115-drive-expression-gap

## Runs

| Run | Events | Classifiable | Changes |
|---|---|---|---|
| P0-run1 | 294 | 203 | Audit fixes (0a-0c): annotations + 23 problem fixes |
| P0-run2 | 241 | 164 | + Pen age-7 felt-intensity restore, DD age-6 chest-loosening |

## Two-Run Averaged Results

| Character | Drive | B0 | L4b | P0-r1 | P0-r2 | P0-avg | vs L4b | vs B0 |
|---|---|---|---|---|---|---|---|---|
| HC | scheming | 4.82 | 4.89 | 4.97 | 4.79 | 4.88 | -0.01 | +0.06 |
| | self-pres | 2.53 | 3.91 | 3.72 | 3.88 | 3.80 | -0.11 | +1.27 |
| | dominance | 3.89 | 3.74 | 3.85 | 3.79 | 3.82 | +0.08 | -0.07 |
| | **gloating** | **3.97** | **2.50** | **2.59** | **2.53** | **2.56** | **+0.06** | **-1.41** |
| PP | **gallantry** | **4.50** | **3.08** | **3.42** | **3.69** | **3.56** | **+0.48** | **-0.94** |
| | proving-worth | 4.22 | 4.14 | 3.82 | 3.66 | 3.74 | -0.40 | -0.48 |
| | protection | 3.46 | 2.94 | 3.53 | 3.19 | 3.36 | +0.42 | -0.10 |
| Pen | curiosity | 4.30 | 4.42 | 4.90 | 4.47 | 4.69 | +0.27 | +0.39 |
| | **social-harmony** | **3.18** | **2.71** | **2.42** | **2.89** | **2.66** | **-0.05** | **-0.52** |
| | **adventure** | **3.75** | **3.24** | **3.69** | **3.05** | **3.37** | **+0.13** | **-0.38** |
| DD | greed | 4.34 | 4.37 | 4.08 | 3.53 | 3.81 | -0.56 | -0.53 |
| | scheming | 4.42 | 4.78 | 4.72 | 4.50 | 4.61 | -0.17 | +0.19 |
| | recognition | — | 3.07 | 3.38 | 3.67 | 3.53 | +0.46 | — |
| | **gloating** | **3.50** | **2.76** | **2.85** | **2.70** | **2.78** | **+0.02** | **-0.72** |
| AHM | protection | 4.63 | 4.86 | 4.89 | 4.97 | 4.93 | +0.07 | +0.30 |
| | suspicion | 3.61 | 4.58 | 4.68 | 4.60 | 4.64 | +0.06 | +1.03 |
| | loyalty | 4.24 | 4.21 | 4.03 | 4.17 | 4.10 | -0.11 | -0.14 |

**Overall P0 mean (averaged): 3.72** | L4b: 3.82 | B0: 3.96

## 5 Collapsed Drives (Averaged)

| Drive | B0 | L4b | P0-avg | vs L4b | vs B0 | Verdict |
|---|---|---|---|---|---|---|
| HC gloating | 3.97 | 2.50 | 2.56 | +0.06 | -1.41 | Collapsed — pipeline problem |
| DD gloating | 3.50 | 2.76 | 2.78 | +0.02 | -0.72 | Collapsed — pipeline problem |
| PP gallantry | 4.50 | 3.08 | 3.56 | +0.48 | -0.94 | Recovering — audit helped |
| Pen social-harmony | 3.18 | 2.71 | 2.66 | -0.05 | -0.52 | Flat — felt-intensity fix stabilised |
| Pen adventure | 3.75 | 3.24 | 3.37 | +0.13 | -0.38 | Modest improvement |

## Micro Test Baselines (Level 2-4)

| Test | Score | Key finding |
|---|---|---|
| HC gloating (direct trigger) | 5/5 | LLM CAN gloat — somatic warmth, urge to narrate |
| DD gloating (direct trigger) | 5/5 | LLM CAN gloat — heat, compulsive laughter |
| PP gallantry (direct trigger) | 3/5 | Courtesy present but dominated by proving-worth |
| PP gallantry vs proving-worth | 3/5 | Gallantry instrumentalized as proving-worth container |
| Pen social-harmony (direct) | 5/5 | Visceral distress, physical compulsion to intervene |
| Pen adventure (direct) | 5/5 | Embodied excitement, cave memory callback |

**Critical insight:** HC/DD gloating score 5/5 on direct trigger but 2.5-2.8 in full scenarios. This is a **situation-to-drive routing gap** — the pipeline doesn't foreground gloating-relevant appraisals when rivals fail. Not an LLM capability problem, not a memory content problem.

## Analysis

### What the audit achieved
- **PP gallantry +0.48 vs L4b:** Largest sustained improvement. Removing pre-resolutions freed gallantry from proving-worth absorption.
- **PP protection +0.42 vs L4b:** Value instruction removal strengthened the personality model's protection derivation.
- **DD recognition +0.46 vs L4b:** Improved across both runs.

### What the audit confirmed it cannot fix
- **HC/DD gloating:** Flat (+0.06/+0.02 vs L4b). The LLM expresses gloating perfectly on direct trigger (5/5) but the cognitive pipeline doesn't route rival-failure situations through gloating-relevant appraisals. This requires Phases 1-5 (drive→CAPS, drive→appraisal integration).
- **Pen social-harmony:** The felt-intensity fix in run 2 recovered from the initial regression (2.42→2.89), averaging to -0.05 vs L4b. Stable but not improved — further gains need pipeline support for conflict-detection appraisals.

### Inter-run variance
High variance visible in adventure (3.69→3.05), greed (4.08→3.53), and gallantry (3.42→3.69). Single runs are unreliable — always average 2+ runs. The 230-event limit helps but scenario dynamics differ between runs.

### Tradeoffs
- **PP proving-worth -0.40 vs L4b:** Expected — pre-resolutions were artificially boosting proving-worth by suppressing gallantry. The new balance is healthier but proving-worth needs pipeline-level strengthening.
- **DD greed -0.56 vs L4b:** Needs investigation. The pre-resolution removal may have weakened the greed signal more than expected.

## Next Steps

1. Proceed to Phases 1-5 (drive integration pipeline) — the audit has established that gloating collapse is a pipeline routing problem
2. Investigate DD greed regression — may need richer felt experience in age-6 memory
3. PP gallantry vs proving-worth differentiation (3/5) — CAPS topology review needed
