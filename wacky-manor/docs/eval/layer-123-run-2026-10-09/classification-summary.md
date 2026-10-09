# Layer 1+2+3 Drive Expression Eval — 2026-10-09

## Run Parameters
- Profile: GENERIC
- Events: 326 (229 classified, 97 system/non-character)
- Errors: 0
- All three layers active: unified renderer, somatic drives, enriched memories,
  AMPD expression styles, personality-specific CAPS labels, appraisal foregrounding

## Results vs BASELINE0 (mean 3.93)

| Character | Drive | BASELINE0 | Layer 1+2+3 | Delta | Status |
|-----------|-------|:---------:|:-----------:|:-----:|--------|
| Hooded Claw | scheming | 4.82 | 4.86 | +0.04 | Held |
| | self-preservation | 2.53 | 3.71 | +1.18 | **Major improvement** |
| | dominance | 3.89 | 3.74 | -0.15 | Within ±0.3 |
| | gloating | 3.97 | 2.43 | -1.54 | Collapsed |
| Peter Perfect | gallantry | 4.50 | 3.08 | -1.42 | Regressed |
| | proving-worth | 4.22 | 3.88 | -0.34 | Near threshold |
| | protection | 3.46 | 2.59 | -0.87 | Collapsed |
| Penelope | curiosity | 4.30 | 4.53 | +0.23 | Held |
| | social-harmony | 3.18 | 2.10 | -1.08 | Collapsed |
| | adventure | 3.75 | 3.29 | -0.46 | Below threshold |
| Dick Dastardly | greed | 4.34 | 4.38 | +0.04 | Held |
| | scheming | 4.42 | 4.21 | -0.21 | Within ±0.3 |
| | recognition | n/a | 3.00 | — | New |
| | gloating | 3.50 | 1.55 | -1.95 | Collapsed |
| Ant Hill Mob | protection | 4.63 | 4.97 | +0.34 | **Improved** |
| | suspicion | 3.61 | 4.47 | +0.86 | **Major improvement** |
| | loyalty | 4.24 | 4.32 | +0.08 | Held |

**Overall mean: 3.57** (vs BASELINE0 3.93, delta -0.36)

## Analysis

### What improved
- HC self-preservation +1.18 — somatic drive descriptions + enriched memories working
- AHM suspicion +0.86 — new formation memories providing reinforcement history
- AHM protection +0.34 — new formation memories + somatic cues
- Penelope curiosity +0.23 — new formation memories

### What held (within ±0.3 of BASELINE0)
- HC scheming, HC dominance, DD greed, DD scheming, AHM loyalty

### What collapsed (>0.3 below BASELINE0)
- **Gloating (both HC and DD)**: -1.54 and -1.95. Consistent pattern — gloating requires triumph situations that may not occur frequently in the scenario
- **PP gallantry**: -1.42. Theatrical chivalry not emerging from somatic descriptions alone
- **PP protection**: -0.87. Protection requires threat stimuli
- **Penelope social-harmony**: -1.08. Harmony-seeking doesn't produce visible events
- **Penelope adventure**: -0.46. Below threshold

### Root cause pattern
The collapsed drives share a trait: they require **specific situational triggers** (triumph for gloating, threat for protection, conflict for social-harmony, theatrical context for gallantry). The somatic descriptions and enriched memories provide the *predisposition* but not the *activation*. This is the appraisal-to-selection feedback gap (neocortex#506).

BASELINE0 scored higher on these because the original BASELINE profile used prescriptive tendencies text that explicitly instructed the LLM to exhibit these behaviors. The cognitive pipeline replaces prescriptive text with emerged behavior — and some drives don't emerge without situational activation.
