# Pipeline Wiring Drive Expression Eval — 2026-10-13

## Run Parameters
- Profile: GENERIC
- Events: 997 (772 classified, 225 system/non-character)
- Errors: 0
- All pipeline wiring active: appraisal-driven drive foregrounding (#118),
  Klinger concern-based salience (#119), CAPS dampening fix (neocortex#510),
  expression styles (neocortex#505), drive-CAPS loop (neocortex#509),
  pipeline tuning (neocortex#511), personality-specific attractor labels (neocortex#504)

## Results vs BASELINE0 (mean 3.93) and Layer 1+2+3 (mean 3.57)

| Character | Drive | BASELINE0 | Layer 1+2+3 | Pipeline Wiring | Δ vs B0 | Δ vs L123 | Status |
|-----------|-------|:---------:|:-----------:|:---------------:|:-------:|:---------:|--------|
| Hooded Claw | scheming | 4.82 | 4.86 | 4.11 | -0.71 | -0.75 | Regressed |
| | self-preservation | 2.53 | 3.71 | 3.04 | +0.51 | -0.67 | Improved vs B0, regressed vs L123 |
| | dominance | 3.89 | 3.74 | 3.26 | -0.63 | -0.48 | Regressed |
| | gloating | 3.97 | 2.43 | 2.28 | -1.69 | -0.15 | Still collapsed |
| Peter Perfect | gallantry | 4.50 | 3.08 | 3.23 | -1.27 | +0.15 | Slight recovery from L123 |
| | proving-worth | 4.22 | 3.88 | 3.27 | -0.95 | -0.61 | Regressed |
| | protection | 3.46 | 2.59 | 2.95 | -0.51 | +0.36 | Recovering from L123 |
| Penelope | curiosity | 4.30 | 4.53 | 3.85 | -0.45 | -0.68 | Below threshold |
| | social-harmony | 3.18 | 2.10 | 2.85 | -0.33 | +0.75 | **Recovering** from L123 |
| | adventure | 3.75 | 3.29 | 2.83 | -0.92 | -0.46 | Regressed |
| Dick Dastardly | greed | 4.34 | 4.38 | 3.23 | -1.11 | -1.15 | **Major regression** |
| | scheming | 4.42 | 4.21 | 3.35 | -1.07 | -0.86 | Regressed |
| | recognition | n/a | 3.00 | 3.04 | — | +0.04 | Stable |
| | gloating | 3.50 | 1.55 | 1.74 | -1.76 | +0.19 | Still collapsed |
| Ant Hill Mob | protection | 4.63 | 4.97 | 4.23 | -0.40 | -0.74 | Regressed from L123 high |
| | suspicion | 3.61 | 4.47 | 3.03 | -0.58 | -1.44 | **Major regression** |
| | loyalty | 4.24 | 4.32 | 4.66 | +0.42 | +0.34 | **Improved** |

**Overall mean: 3.24** (vs BASELINE0 3.93 Δ-0.69, vs Layer 1+2+3 3.57 Δ-0.33)

## Critical Confound: Run Length

This run produced 997 events (772 classified) vs 326 events (229 classified) in the
Layer 1+2+3 run. Sample sizes per character are 123-196 vs ~46 in L123. Longer runs
contain more routine dialogue and filler interactions where drives are weakly expressed,
mechanically pulling averages down. **The drop is not directly comparable** to shorter runs.

## Analysis

### What improved (vs Layer 1+2+3)
- **Penelope social-harmony**: 2.10 → 2.85 (+0.75) — strongest recovery, was most collapsed
- **AHM loyalty**: 4.32 → 4.66 (+0.34) — now above BASELINE0
- **PP protection**: 2.59 → 2.95 (+0.36) — recovering from collapse
- **PP gallantry**: 3.08 → 3.23 (+0.15) — slight recovery
- **DD gloating**: 1.55 → 1.74 (+0.19) — still deeply collapsed but trending up

### What regressed (vs Layer 1+2+3)
- **AHM suspicion**: 4.47 → 3.03 (-1.44) — largest drop, was a major L123 success
- **DD greed**: 4.38 → 3.23 (-1.15) — was stable in L123, now dropping
- **DD scheming**: 4.21 → 3.35 (-0.86) — was stable in L123
- **HC scheming**: 4.86 → 4.11 (-0.75) — still above 4.0 but declining
- **Penelope curiosity**: 4.53 → 3.85 (-0.68) — was a strength

### Root cause hypotheses

1. **Run-length dilution**: With 3x more events, average scores are diluted by routine
   interactions. Need a length-normalized comparison (e.g., score only first 326 events)
   to isolate pipeline wiring effects from run length.

2. **Salience over-activation**: Klinger concern-based salience may be activating too many
   drives simultaneously, spreading attention across drives and reducing peak expression of
   any single drive. The AHM suspicion drop (4.47→3.03) is suspicious — was this a drive
   that benefited from focused attention and is now competing with other activated drives?

3. **Drive-CAPS interference**: The drive adaptation → CAPS settling loop may be dampening
   drives that were previously expressed without constraint. The settling process could
   converge drives toward moderate expression levels.

### Recommended next steps

1. **Run length-controlled comparison**: Classify only the first ~230 events of this
   transcript to match L123 sample size. This isolates pipeline effects from run length.

2. **If length-normalized scores still drop**: Investigate Klinger salience specificity —
   the concern matching may be too broad, activating all drives on every observation.

3. **If length-normalized scores recover**: The pipeline is working; the eval methodology
   needs to account for run length (events-per-minute rate, or fixed-window scoring).
