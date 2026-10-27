# Eval Directory — Wacky Manor Scenario Runs & Instruments

Scenario run transcripts, classification results, and eval instruments for the cognitive character architecture. Runs are chronological, grouped by the thinking-field text version active at the time.

## Baselines

| Name | Run | Profile | Scoring | Mean | Notes |
|------|-----|---------|---------|:----:|-------|
| **BASELINE0** | `generic-colloquial-20261002` | GENERIC | flat | 3.93 | Original full-brief run. **Inflated** — flat scoring gave credit for drive contamination (P50, P75). Has `classification.json`. |
| **BASELINE1** | `emotional-core-baseline1-generic-20261004` | GENERIC | flat | 4.06 | L1 emotional echo instruction. Has `classification.json`. Improved 13/17 drives, regressed 4. |
| **BASELINE2** | (phase0-audit-baseline-2026-10-13) | BASELINE | focused eval | 3.57 | Pipeline architectural ceiling. Quality redistributed, not created (P75). |

## Scenario Runs

### Pre-thinking-field capture (Sep 27 – Oct 3)

No thinking fields captured. Dialogue and action events only. Useful for drive expression eval (classify_emotions.py) but not thinking-field scoring.

| Directory | Date | Profile | Events | Classification | Notes |
|-----------|------|---------|:------:|:-:|-------|
| `emergence-20260927-232108` | Sep 27 | ? | 0 | — | Empty run files, has emergence-report.md |
| `event-driven-20260928-045431` | Sep 28 | ? | 0 | — | Empty |
| `directive-minimal-20261002` | Oct 02 | ? | 0 | — | No transcript |
| `old-briefings-20261002` | Oct 02 | BASELINE | 327 | — | Old briefing format |
| `new-briefings-20261002` | Oct 02 | BASELINE | 333 | — | New briefing format |
| `generic-briefings-20261002` | Oct 02 | GENERIC | 306 | — | Generic briefing format |
| `generic-colloquial-20261002` | Oct 02 | GENERIC | 310 | flat | **= BASELINE0.** Full briefs, colloquial style |
| `pareback-r1-baseline-20261003` | Oct 03 | BASELINE | 331 | — | Pareback round 1 |
| `pareback-r1-generic-20261003` | Oct 03 | GENERIC | 315 | — | |
| `pareback-r2-baseline-20261003` | Oct 03 | BASELINE | 311 | — | |
| `pareback-r2-generic-20261003` | Oct 03 | GENERIC | 334 | — | |
| `pareback-r2b-baseline-20261003` | Oct 03 | BASELINE | 315 | — | |
| `pareback-r2b-generic-20261003` | Oct 03 | GENERIC | 322 | — | |
| `pareback-r3-generic-20261003` | Oct 03 | GENERIC | 325 | flat | R3 = no instruction, tendencies stripped |
| `pareback-r3b-generic-20261003` | Oct 03 | GENERIC | 312 | flat | R3b = passive review instruction |
| `pareback-r3c-generic-20261003` | Oct 03 | GENERIC | 306 | — | R3c variant |
| `pareback-r3d-generic-20261003` | Oct 03 | GENERIC | 186 | — | R3d variant (short run) |
| `pareback-r3f-generic-20261003` | Oct 03 | GENERIC | 301 | — | R3f variant |
| `pareback-r3g-generic-20261003` | Oct 03 | GENERIC | 129 | — | R3g = minimal evocative (short) |
| `pareback-r3g-full-generic-20261004` | Oct 04 | GENERIC | 309 | flat | R3g full run |
| `emotional-core-generic-20261003` | Oct 03 | GENERIC | 69 | — | Short run |
| `emotional-core-generic-full-20261003` | Oct 03 | BASELINE | 375 | — | Full emotional core run |

### V0 — Emotional echo (Oct 4–6)

Thinking-field text: `"What are you FEELING right now — not thinking, feeling? Name it."`

| Directory | Date | Profile | Events | Think | Classification | TF Scores | Notes |
|-----------|------|---------|:------:|:-----:|:-:|:-:|-------|
| `layer1-thinking-capture-generic-20261004` | Oct 04 | GENERIC | 318 | 222 | flat | yes | First run with thinking capture |
| `layer1-emotional-echo-generic-20261004` | Oct 04 | GENERIC | 320 | 0 | — | — | Thinking not captured (bug?) |
| `layer1-long-run-generic-20261004` | Oct 04 | GENERIC | 732 | 520 | flat | — | Long run, L1 instruction |
| `emotional-core-baseline1-generic-20261004` | Oct 04 | GENERIC | 336 | 232 | flat | — | **= BASELINE1** |

### V1 — Somatic grounding (Oct 7–21)

Thinking-field text: `"What do you feel in your body right now? A tightness, a warmth, a prickle, a numbness? Start there — in the body, not the head."`

| Directory | Date | Profile | Events | Think | Classification | TF Scores | RESULTS | Notes |
|-----------|------|---------|:------:|:-----:|:-:|:-:|:-:|-------|
| `generic-appraisal-20261007` | Oct 07 | GENERIC | 1227 | 842 | — | — | — | Large appraisal run |
| `stripped-drives-session-reuse-generic-20261007` | Oct 07 | GENERIC | 866 | 635 | flat | — | — | Session reuse test |
| `pipeline-hardening-run-2026-10-08` | Oct 08 | BASELINE | 2024 | 1550 | — | — | — | Largest run (2024 events) |
| `generic-pipeline-hardening-20261008` | Oct 08 | GENERIC | 230 | 151 | flat | — | — | |
| `layer-123-run-2026-10-09` | Oct 09 | BASELINE | 332 | 229 | — | yes | — | Layers 1-2-3 combined |
| `phase0-audit-baseline-2026-10-13` | Oct 13 | BASELINE | 294 | 203 | — | yes | yes | **= BASELINE2** |
| `phase0-fix-run-2026-10-13` | Oct 13 | GENERIC | 241 | 161 | — | — | — | Post-fix verification |
| `layer4-gallantry-fix-2026-10-13` | Oct 13 | BASELINE | 319 | 215 | — | — | — | Gallantry fix |
| `layer4-refinement-run-2026-10-13` | Oct 13 | BASELINE | 349 | 239 | — | — | — | |
| `pipeline-wiring-run-2026-10-13` | Oct 13 | BASELINE | 997 | 773 | — | — | — | Large pipeline test |
| `phase1-run-2026-10-14` | Oct 14 | BASELINE | 290 | 199 | — | — | — | |
| `phase1-pipeline-run-2026-10-14` | Oct 14 | GENERIC | 195 | 134 | — | — | — | |
| `multichannel-pipeline-2026-10-14` | Oct 14 | BASELINE | 900 | 660 | — | — | yes | Multichannel pipeline verified |
| `phase1-full-pipeline-run-2026-10-14` | Oct 14 | GENERIC | 899 | 670 | — | — | — | |
| `phase1-verified-run-2026-10-14` | Oct 14 | BASELINE | 457 | 327 | — | — | — | |
| `layer-b-eval-2026-10-14` | Oct 14 | — | 0 | 0 | — | — | yes | Micro test only (no transcript) |
| `pre-inference-2026-10-14` | Oct 14 | — | 0 | 0 | — | — | — | Empty |
| `appraisal-filtering-2026-10-15` | Oct 15 | BASELINE | 321 | 217 | — | yes | yes | Appraisal filter validated |
| `llm-classifier-2026-10-15` | Oct 15 | BASELINE | 306 | 211 | — | — | — | |
| `caps-descriptions-2026-10-17` | Oct 17 | BASELINE | 300 | 201 | — | yes | yes | CAPS descriptions added |
| `bugfixes-pinned-2026-10-17` | Oct 17 | BASELINE | 324 | 219 | — | — | — | |
| `fixa-verification-2026-10-18` | Oct 18 | BASELINE | 126 | 84 | — | — | — | Short verification |
| `instrumented-50evt-2026-10-18` | Oct 18 | BASELINE | 86 | 60 | — | — | — | Instrumented diagnostic |
| `degree-accumulation-validation-2026-10-21` | Oct 21 | BASELINE | 307 | 203 | — | yes | — | Degree accumulator verified |

### V2 — Verbose multi-channel (Oct 22–26)

Thinking-field text: `"What's happening inside you right now? Notice your body — a tightness, a warmth, a numbness. Notice what's grabbing your attention..."`

| Directory | Date | Profile | Events | Think | TF Scores | Notes |
|-----------|------|---------|:------:|:-----:|:-:|-------|
| `multi-channel-smoke-2026-10-22` | Oct 22 | GENERIC | 128 | 86 | — | Smoke test for multi-channel |
| `full-cognitive-run1-2026-10-22` | Oct 22 | BASELINE | 193 | 126 | — | Short run |
| `tuned-run-2026-10-22` | Oct 22 | BASELINE | 514 | 360 | yes | Externalized config, session pipeline |
| `full-cognitive-run-2026-10-26` | Oct 26 | BASELINE | 303 | 212 | yes | Full pipeline, inline mood |

### V3 — Condensed multi-channel (Oct 27+)

Thinking-field text: `"your private inner monologue — attend to what feels vivid: body (tension, warmth), attention (what pulls, what you avoid)..."`

| Directory | Date | Profile | Events | Think | TF Scores | Notes |
|-----------|------|---------|:------:|:-----:|:-:|-------|
| `thinking-field-run-2026-10-27` | Oct 27 | GENERIC | 1181 | 834 | yes | 300-tick run, #154 |

## Eval Instruments

### Scripts

| Script | Purpose | Input | Output |
|--------|---------|-------|--------|
| `classify_emotions.py` | Drive expression scoring (flat or relevance-gated) | transcript dir | per-character per-drive scores |
| `score_thinking_field.py` | Thinking-field phenomenological quality (4 dimensions) | transcript dir | per-character aggregate scores |
| `cross_compare.py` | Cross-run comparison | multiple transcript dirs | comparison table |
| `correlate_instrumentation.py` | Correlate instrumentation metrics with events | instrumented log + transcript | correlation analysis |
| `long_run_analysis.py` | Analyse long-run temporal dynamics | transcript dir | temporal analysis |
| `r3b_analysis.py` | R3b pareback variant analysis | transcript dir | R3b-specific metrics |
| `test_appraisal.py` | Appraisal filter test harness | — | test results |

### Java eval tests (in `wacky-manor/src/test/.../experiment/`)

| Test | Tag | Purpose |
|------|-----|---------|
| `ThinkingFieldEvalTest` | llm-eval | Controlled A/B: plain vs calibrated thinking field, 4 scenarios × N=3 |
| `BehavioralAttractorEvalTest` | llm-eval | Opposing attractor steering, confrontation vs harmony |
| `ChannelWeightingEvalTest` | llm-eval | Situation-dependent channel weight validation |

### Neocortex eval tests (in `neocortex/caps-llm-classifier/.../eval/`)

| Test | Tag | Purpose |
|------|-----|---------|
| `FocusedDriveEvalTest` | llm-eval | 10 scenarios (S1-S10), drive expression quality per scenario |

## Standalone Results

| File | Purpose |
|------|---------|
| `thinking-field-eval-2026-10-27.json` | ThinkingFieldEvalTest controlled A/B results |
| `relational-model-eval-*.json` | Personality emergence eval results (14 tests, score 0-5) |
| `sleep-derivation-eval-*.json` | Sleep derivation eval results (9 dimensions, score 0-5) |
| `full-run-2026-10-08.json` | Full scenario run summary |
| `cognitive-snapshots-2026-10-07.jsonl` | Per-tick cognitive state snapshots |
| `classification-audit.jsonl` | CAPS classification audit log |
| `RELATIONAL-MODEL-EVAL-TRACKER.md` | Tracks relational model eval scores across runs |

## Per-Run Artifacts

Each run directory may contain:

| File | Description |
|------|-------------|
| `transcript.json` | Full event transcript (`{complete, reason, events}` or event array) |
| `classification.json` | Drive expression classification (flat method) |
| `thinking-field-scores.json` | Thinking-field quality scores (4 dimensions, per-character) |
| `RESULTS.md` | Human-written analysis of the run's findings |
| `micro-test-scores.json` | Focused micro-test results |

## Profile Note

Directory names containing "generic" used the GENERIC profile (renamed characters, no pop-culture refs). Others used BASELINE (Wacky Races characters). The agent IDs (`penelope-pitstop`, `hooded-claw`, etc.) are the same in both profiles — only display names differ. Profile detection from transcript content is unreliable; use the directory name as the authority.
