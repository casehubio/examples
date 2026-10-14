# Multichannel Pipeline Run — 2026-10-14

## What Was Fixed

Three pipeline gaps blocking drive discrimination in full scenario runs (neocortex #584, #585, #586) plus a rendering order fix in examples (#122).

### Pipeline Fixes (neocortex)

**#584 — DriveProfileDerivationPhase produces 0 profiles**
- Root cause: early-return guard when `SituationClassifier`/`CapsTopology` CDI `Instance<>` deps don't resolve during consolidation
- Fix: graceful degradation — profiles derive from memory excerpts alone; competition detected via shared `catalogue-entry-id` overlap instead of shared CAPS mediating nodes
- Verified: 10/10 tests, profiles confirmed in full scenario (PP: 3 profiles, 2 competitions)

**#586 — Somatic discrimination text not generating**
- Root cause: cascading from #584 — 0 competition artifacts meant `generateDiscriminationText()` had nothing to process
- Fix: cross-phase pipeline test confirms Phase 1 → Phase 2 data flow works end-to-end
- Verified: competition nodes updated with LLM-generated multi-channel discrimination text

**#585 — Generation prompt somatic-only strips discriminating channels**
- Root cause: both `SYSTEM_PROMPT` and `DISCRIMINATION_SYSTEM` restricted to "somatic/bodily sensations only"
- Fix: updated to Barrett (2017) 4-channel constructed emotion — somatic, attentional, temporal, action tendency
- Literature: Barrett (2017) constructed emotion, Frijda (1986) action tendencies, Easterbrook (1959) attentional narrowing

### Rendering Fix (examples)

**Strength-order rendering**
- Root cause: activated drives rendered before ambient drives regardless of intensity — gallantry (0.9) was buried after protection (0.8) when protection was the activated category
- Fix: all drives now render in intensity-descending order. `activatedCategories` still used for situation priming but no longer controls description position
- Principle: **within any section of the cognitive observation, items must appear in strength/priority order**. LLMs attend more to what they read first. This is a meta-rule that applies beyond drives.

### Observability (neocortex)

Added pipeline logging at consolidation time:
- `ConsolidationScheduler`: phase start/end with millisecond timing
- `DriveProfileDerivationPhase`: per-agent profile count, memory count, CAPS vs catalogue mode, competition pairs
- `DriveDescriptionGenerationPhase`: per-agent progress (X/Y), per-drive memory count + LLM timing

## What Works

### Multi-channel descriptions discriminate

The 4-channel prompt produces genuinely different descriptions for competing drives. PP example from this run:

| Channel | Gallantry | Proving-worth |
|---------|-----------|---------------|
| Somatic | chest fills with sharp bright heat, spine locks straight | tightness gathers across forearms and hands |
| Attentional | world narrows to *the person exposed* | attention locks onto *the gap: the thing broken, not yet built* |
| Temporal | time crystallizes into a hinge-point | time narrows to a corridor between now and proof |
| Action tendency | body leans forward, crossing toward the gap | fingers ache with readiness, mind disassembling into parts |

These are distinguishable — gallantry is person-oriented and urgent, proving-worth is task-oriented and patient.

### Competition detection works without CAPS

The catalogue-based fallback correctly detects PP gallantry vs proving-worth competition via shared `other_safety` catalogue entry. Both drives share the same experiential territory (protecting/helping others) but express through different channels.

### Discrimination text is LLM-generated

Phase 2 successfully reads Phase 1's competition nodes and generates multi-channel discrimination criteria:
> "One pull arrives as a hot surge forward through the chest, attention narrowing sharply onto another person... The other settles as a bracing across the shoulders and forearms, attention widening to scan what could be made stronger..."

### Micro test confirms quality

PP gallantry door-stuck micro test: **4/5** (3 runs: 4, 3, 4). Up from 2/5 pre-pipeline baseline. The LLM can discriminate gallantry from proving-worth when presented with the multi-channel descriptions.

## What Doesn't Work

### Full scenario gallantry still at 2.50

| Character | Drive | @300 events | B0 baseline | Delta |
|-----------|-------|-------------|-------------|-------|
| PP | gallantry | **2.50** | 3.96 | **-1.46** |
| PP | proving-worth | 4.00 | 3.96 | +0.04 |
| PP | protection | 3.89 | 3.96 | -0.07 |
| DD | scheming | 4.44 | 3.96 | +0.48 |
| DD | gloating | **2.85** | 3.96 | **-1.11** |
| HC | scheming | 5.00 | 3.96 | +1.04 |
| HC | gloating | **2.37** | 3.96 | **-1.59** |
| Pen | curiosity | 4.45 | 3.96 | +0.49 |
| Pen | adventure | **2.84** | 3.96 | **-1.12** |

**Overall mean: 3.68** vs B0 3.96. Micro tests score well (4/5) but full scenario averages are low for specific drives.

The gap between micro test (4/5) and full scenario (2.50) suggests the issue is not description quality but **how often the drive gets opportunity to express**. In a 300-event scenario, most events don't trigger gallantry situations — PP is often in rooms without Clara, or solving puzzles (proving-worth territory).

### Scores decay past 300 events

Everything drops after ~300 events. Characters exhaust scripted content and drift. Scheming goes from 5.0→3.6, greed from 4.15→2.97. The scenario has no termination signal — needs a boredom/satiation mechanism from neocortex to detect when novelty drops below threshold.

### Below-par drives share a pattern

The consistently low drives — gallantry, gloating, adventure — are all **reactive** drives. They need specific situational triggers (someone in trouble, a rival failing, something unknown discovered). The high drives — scheming, curiosity, proving-worth — are **proactive** drives that the character can pursue in any situation.

This suggests the next improvement isn't in the description pipeline but in the **scenario design** or **situation priming** — ensuring each character encounters enough trigger situations for their reactive drives.

## Meta-Rule: Strength-Ordered Rendering

**Within any multi-item section of the cognitive observation, items must appear in strength/priority order.**

This is a rendering principle, not a content change. LLMs attend more to what they read first (primacy bias). Currently applies to:
- "What Pulls at You" — drives by intensity (fixed this session)

Should also apply to:
- "Your Beliefs" — by confidence or recency
- "Social Rules" — by priority weight
- Situation-primed memories — by relevance score (already implemented)
- CAPS attractors — by strength (already implemented)

## Files

- `transcript.json` — 900 events, full run
- `cognitive-dump-*.json` — per-character pipeline state
- `window-300/` — first 300 events (B0 comparison window)

## Next Steps

1. **Strength-order all sections** — apply the meta-rule to beliefs and social rules
2. **Scenario termination** — boredom/satiation signal from neocortex (separate feature)
3. **Reactive drive activation** — ensure scripted scenarios include enough trigger situations for gallantry, gloating, adventure
4. **Re-run with strength-order fix** — the rendering fix was made after this run; need fresh 300-event comparison
