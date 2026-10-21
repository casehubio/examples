# Degree Accumulation Validation Run — 2026-10-21

## Summary

First end-to-end validation of demand-driven classification (#652).
GENERIC profile, 5 characters, 23 ticks.

## Results

| Metric | Value |
|--------|-------|
| Ticks | 23 |
| Accumulator firings | 22 |
| Assessments received | 20 |
| Missed assessments | 0 |
| Success rate | 100% |

### Per-character firing frequency

| Character | Firings | Avg ticks/firing |
|-----------|---------|------------------|
| ant-hill-mob | 6 | 3.8 |
| peter-perfect | 6 | 3.8 |
| penelope-pitstop | 4 | 5.8 |
| hooded-claw | 4 | 5.8 |
| dick-dastardly | 2 | 11.5 |

### Accumulator parameters (defaults — no tuning needed)

| Parameter | Value | Purpose |
|-----------|-------|---------|
| allowance (k) | 0.2 | CUSUM slack — degree < 0.2 is noise |
| threshold (h) | 1.0 | Accumulated level that triggers classification |
| decay (λ) | 0.85 | Leaky integrator — stale evidence decays |

## Observations

1. **Firing frequency is narrative-appropriate.** High-drama characters (protector, hero) fire every 3-4 ticks. Low-drama characters (scheming villain) fire every 10+ ticks. This emerges from the LLM's self-assessed `situationChangeDegree`.

2. **Inline classification works reliably.** Zero missed assessments — when the accumulator fires and the assessment instruction is appended to the prompt, the LLM produces a valid `situationAssessment` every time.

3. **Assessment quality is rich.** 7-8 CAPS node activations per assessment, with contextual reasoning referencing specific scenario events.

4. **CAPS appraisal filter integration confirmed.** Drive overlap scores are character-appropriate (e.g., dick-dastardly's greed=0.988 with failure/competition situation, ant-hill-mob's protection=0.618 with aversive_removal).

5. **No parameter tuning needed.** The defaults produce well-distributed classification across characters without over- or under-classifying.

## Files

- `transcript.json` — full scenario event transcript (445KB)
- `degree-accumulation.log` — per-tick degree values, accumulator levels, and classification events
