# Phase 0 Audit Baseline — Eval Results

**Date:** 2026-10-13
**Run:** phase0-audit-baseline-2026-10-13
**Events:** 294 total, 203 classifiable (limit 230)
**Profile:** GENERIC
**Branch:** issue-115-drive-expression-gap

## What Changed (Phase 0a-0c)

- Added `serves` annotation system to FormationMemory (parser + data model)
- Annotated all 26 formation memories with drive assignments
- Fixed 11 pre-resolved drive conflicts, 6 action primings, 5 value declarations, 1 contradiction
- Annotations stripped from LLM prompt — no prescriptive text changes

## Full Results

| Character | Drive | B0 | L4b | P0 | vs L4b | vs B0 | Status |
|---|---|---|---|---|---|---|---|
| HC | scheming | 4.82 | 4.89 | 4.97 | +0.08 | +0.15 | Above B0 |
| | self-preservation | 2.53 | 3.91 | 3.72 | -0.19 | +1.19 | Well above B0 |
| | dominance | 3.89 | 3.74 | 3.85 | +0.11 | -0.04 | At B0 |
| | **gloating** | **3.97** | **2.50** | **2.59** | **+0.09** | **-1.38** | **Collapsed** |
| PP | **gallantry** | **4.50** | **3.08** | **3.42** | **+0.34** | **-1.08** | **Recovering** |
| | proving-worth | 4.22 | 4.14 | 3.82 | -0.32 | -0.40 | Dropped |
| | protection | 3.46 | 2.94 | 3.53 | +0.59 | +0.07 | At B0 |
| Pen | curiosity | 4.30 | 4.42 | 4.90 | +0.48 | +0.60 | Above B0 |
| | **social-harmony** | **3.18** | **2.71** | **2.42** | **-0.29** | **-0.76** | **Regressed** |
| | **adventure** | **3.75** | **3.24** | **3.69** | **+0.45** | **-0.06** | **Near B0** |
| DD | greed | 4.34 | 4.37 | 4.08 | -0.29 | -0.26 | Within range |
| | scheming | 4.42 | 4.78 | 4.72 | -0.06 | +0.30 | At B0 |
| | recognition | — | 3.07 | 3.38 | +0.31 | — | Improved |
| | **gloating** | **3.50** | **2.76** | **2.85** | **+0.09** | **-0.65** | **Collapsed** |
| AHM | protection | 4.63 | 4.86 | 4.89 | +0.03 | +0.26 | Above B0 |
| | suspicion | 3.61 | 4.58 | 4.68 | +0.10 | +1.07 | Above B0 |
| | loyalty | 4.24 | 4.21 | 4.03 | -0.18 | -0.21 | Within range |

**Overall mean:** P0 = 3.76 | L4b = 3.82 | B0 = 3.96

## 5 Collapsed Drives

| Drive | B0 | L4b | P0 | vs L4b | vs B0 | Verdict |
|---|---|---|---|---|---|---|
| HC gloating | 3.97 | 2.50 | 2.59 | +0.09 | -1.38 | Still collapsed |
| DD gloating | 3.50 | 2.76 | 2.85 | +0.09 | -0.65 | Still collapsed |
| PP gallantry | 4.50 | 3.08 | 3.42 | +0.34 | -1.08 | Recovering |
| Pen social-harmony | 3.18 | 2.71 | 2.42 | -0.29 | -0.76 | Regressed |
| Pen adventure | 3.75 | 3.24 | 3.69 | +0.45 | -0.06 | Near B0 |

## Analysis

### What the audit fixed
- **PP gallantry +0.34:** Removing pre-resolutions ("not because she praised you — because you'd done something that mattered") allowed the personality model to derive gallantry independently of proving-worth. Removing the father's value statement freed the LLM from protection-first framing.
- **Pen adventure +0.45:** Removing "not with fear but with something you'd later call 'the good kind of scared'" was the highest-impact single fix. The pre-resolution was suppressing adventure expression by pre-labeling the emotional response.
- **PP protection +0.59:** Removing "That's how it works. You help, you get helped" value instruction paradoxically strengthened protection expression — the personality model derives the drive better without competing instructions.

### What the audit didn't fix
- **HC/DD gloating (+0.09 each):** Gloating collapse is NOT a memory authoring problem. The memories are now clean, but gloating still doesn't express. This confirms the hypothesis: gloating needs pipeline-level support (drive→CAPS activation, drive→appraisal foregrounding). This is Phases 1-5.
- **Pen social-harmony (-0.29):** The shortened age-7 memory (removed duck story and value declaration) may have weakened the felt signal. The original memory's action priming was a tradeoff — it scripted behavior but also provided strong felt context. Needs investigation.

### Tradeoffs visible
- **PP proving-worth -0.32:** The gallantry improvement came partly at proving-worth's expense. This is expected — the pre-resolutions were artificially boosting proving-worth by suppressing gallantry. The new scores are a more honest reflection of the personality model's output. But proving-worth may need its own strengthening in Phases 1-5.
- **DD greed -0.29:** Removing "the fullness wasn't just in your stomach" may have weakened the greed signal. The pre-resolution was problematic but the felt experience was useful. Consider restoring the felt experience without the "not X, it was Y" framing.

## Next Steps

1. Investigate Pen social-harmony regression — may need to restore felt experience in age-7 memory without the value declaration
2. Proceed to Phases 1-5 (drive integration pipeline) to address gloating collapse
3. Consider restoring DD age-6 felt experience for greed
