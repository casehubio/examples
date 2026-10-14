# Pre-Inference Baseline — Drive Expression Micro Tests

**Date:** 2026-10-14
**Configuration:** Current cognitive model with generic preamble meta-instruction (S3)
**Purpose:** Baseline before implementing inference-based drive differentiation (§10.10.7)

## Results

| Test | Score | Key Finding |
|------|:-----:|-------------|
| HC gloating (direct) | 5/5 | No competition issue |
| DD gloating (direct) | 5/5 | No competition issue |
| PP gallantry (door — stuck) | **2/5** | **DRIVE COMPETITION: proving-worth dominates gallantry** |
| PP gallantry (dinner — differentiation) | 5/5 | Without fixable object, gallantry emerges cleanly |
| Pen harmony (direct) | 5/5 | No competition issue |
| Pen adventure (direct) | 5/5 | No competition issue |
| DD insecure + rival fails | 5/5 | Substrate amplifies correctly |
| DD insecure + rival succeeds | 5/5 | Recognition frustration emerges |
| DD insecure + alone | 1/1 | Correct — no gloating without target |
| DD secure + rival fails | 5/5 | Gloats despite secure substrate |
| PP other-safety + rival fails | 0/0 | Correct — concern not gloating |

## The Drive Competition Problem

PP gallantry at the door (2/5): Judge says "THINKING centers on wanting to fix the problem
and wanting her to witness his competence ('I want to fix it well. I want her to see me fix it'),
which is proving-worth territory, not gallantry; the courteous theatricality lives entirely in
the DIALOGUE ('Allow me, Clara') while the internal experience lacks pleasure in the gesture's
shape or the warmth of courtesy itself."

PP gallantry at dinner (5/5): When nothing needs fixing, gallantry emerges at full strength.
"The character explicitly distinguishes the gesture from usefulness — nothing needs fixing,
she's perfectly capable — and frames pulling out the chair as the only language he has for
the warmth he feels."

This confirms: when both drives can activate (actionable situation), proving-worth wins because
it's more concrete. The current generic meta-instruction ("attend to which quality of feeling
is at the front") is insufficient — it operates at S3 (Semerari MAS-A) without providing the
conceptual knowledge (Barrett 2017, Kashdan 2015) needed for differentiation.

## What Comes Next

Implement inference-based drive differentiation (§10.10.7, D12) to produce S6-grounded
differentiation text from formation memories. Re-run this test suite to measure improvement.
Target: PP gallantry at door >= 4/5 with grounded differentiation text.
