# Classification Validation Run — 2026-10-21 (run 2)

## Run Parameters

- **Profile:** GENERIC
- **Mode:** autonomous
- **Active characters:** penelope-pitstop, hooded-claw, ant-hill-mob, dick-dastardly, peter-perfect
- **Classification mode:** all inline (no external characters)
- **Audit log:** enabled, path `docs/eval/classification-audit.jsonl`
- **Accumulator defaults:** k=0.2, h=1.0, lambda=0.85

## Results

- **Completion:** partial (hung in consolidation at ~tick 42)
- **Total events:** 639
- **Audit log entries:** 41 (all inline mode)
- **Classification firing rate:** ~1 per tick (healthy)
- **Characters classified:** all 5

## Analysis

Classifications fire reliably. Degree values range 0.2-0.8 per tick,
accumulator fires approximately every 4-6 ticks per character. Audit
JSONL contains full trajectory, activations with source ranges, and
reasoning for each classification.

Run hung during second consolidation phase (~tick 100). Process showed
0% CPU and no active TCP connections — likely a deadlock or timeout
in the consolidation pipeline.

## Note on audit log path

The audit log path `docs/eval/classification-audit.jsonl` is CWD-relative.
Quarkus dev mode CWD is the Maven module directory (`wacky-manor/`), so
the actual file is at `wacky-manor/docs/eval/classification-audit.jsonl`.
