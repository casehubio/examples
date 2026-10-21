# Classification Validation Run — 2026-10-21

## Run Parameters

- **Profile:** GENERIC
- **Mode:** autonomous
- **Active characters:** penelope-pitstop, hooded-claw, ant-hill-mob, dick-dastardly, peter-perfect
- **Classification mode:** all inline (no external characters)
- **Audit log:** enabled, path `docs/eval/classification-audit.jsonl`
- **Max turns:** 300
- **Accumulator defaults:** k=0.2, h=1.0, lambda=0.85

## Results

- **Completion:** DAWN (normal termination)
- **Total events:** 1071
- **Classification firings:** 0
- **Audit log entries:** 0

## Analysis

The degree accumulator never fired during the entire 300-tick run.
This means the LLM consistently returned `situationChangeDegree` values
at or near 0.0 for all characters across all ticks.

The accumulator formula `S = max(0, S * 0.85 + degree - 0.2)` requires
sustained degree above 0.2 to accumulate. With the default threshold
of 1.0, a constant degree of 0.5 takes ~5 ticks to fire. Degree values
near 0.0 are immediately absorbed by the allowance parameter.

## Follow-up

The previous validation run (same session, earlier) produced classifications
with the GENERIC profile. Possible causes for the difference:

1. **LLM response variability** — the degree field is subjective and
   model-dependent. Different runs with the same prompt can yield
   different degree distributions.
2. **Consolidation cache state** — this run did not reuse the consolidation
   cache from the prior run. The user flagged this as a requirement;
   future runs should preserve the cache.
3. **Accumulator tuning** — the k=0.2 allowance may be too aggressive
   for the typical degree distribution from this model.

Recommended next step: run with verbose degree logging to capture the
actual degree values per tick, then adjust accumulator parameters if
the distribution is systematically low.
