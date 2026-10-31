# ERA6 Phase 1 — Empirical Memory System Assessment

**Date:** 2026-10-31
**Issue:** casehubio/neocortex#737
**Profile:** GENERIC
**Method:** Runtime snapshot of MindMap, CaseMemoryStore, and consolidation output

## Summary

After context clear, a character retains only its static personality profile (seeded at startup). All experiential knowledge — what happened, who said what, emotional reactions — is lost. The character would have complete amnesia about everything that occurred during the scenario.

## Infrastructure Status

| Component | Implementation | Status |
|-----------|---------------|--------|
| MindMapStore | InMemoryMindMapStore | Working |
| CaseMemoryStore | InMemoryMemoryStore | Working (transitive dep) |
| ExperienceRecorderCore | via memory-core | Working but domain mismatch |
| ConsolidationScheduler | 10 phases | Working at startup, blocked at runtime |

## What Gets Seeded (Per Character, e.g. Clara Bellingham)

**93 MindMap nodes total:**

| Category | Count | Source | Notes |
|----------|-------|--------|-------|
| Drive intensities | 6 | drive-adaptation | Baseline values with body-location + channel-emphasis |
| Personality facets | 4 | personality-seed | Big Five subscales (suspiciousness, callousness, etc.) |
| Attachment style | 2 | personality-seed | Anxiety + avoidance scores (duplicated) |
| Need satisfaction | 10 | need-satisfaction | 5 Maslow-inspired tiers (duplicated) |
| Relational schemas | 12 | manor-seed | Trust/intimacy/utility per relationship (duplicated) |
| Formative experiences | 10 | experience-consolidation | 5 backstory episodes graduated from memory (duplicated) |
| Drive profiles | 3 | LLM-inferred | Per-drive behavioral pattern insights |
| Drive competitions | 3 | LLM-inferred | How drives interact/compete |
| CAPS behavioral | 25 | CapsGenerated | Behavioral attractor weights (fight, flight, trust, etc.) |
| Social overlays | 18 | manor-seed | Relationship overlays + entity nodes |

**5 formation memories** in CaseMemoryStore (experience domain):
- Ages 5, 7, 9, 11, 13 — covering curiosity, social-harmony, adventure drives
- These ARE graduated to MindMap via ExperienceConsolidationPhase

## Critical Bugs Found

### Bug 1: Domain Mismatch (High Severity)

`ExperienceRecorderCore` stores ALL memories (formation + runtime) in domain `"experience"` (via `ExperienceEvents.DOMAIN`).

`AgentExperienceService.recall()` queries domain `"manor"` (via `MANOR_DOMAIN`).

These are different `MemoryDomain` instances. Since `InMemoryMemoryStore` buckets by `(tenantId, subjectId, domain)`, recall NEVER finds runtime memories. Characters cannot recall their own experiences.

**Impact:** Even when runtime experience recording works (autonomous mode), the memories are stored but invisible to recall. The character's observation builder gets empty memory lists.

### Bug 2: Scripted Mode Skips Memory Entirely

In `PlaybookOrchestrator.runScripted()` (line 957), `CharacterAgentLoop` is invoked with `experienceService = null`. The loop checks `if (experienceService != null)` before ingesting, so scripted mode records zero runtime experience.

Autonomous mode uses `cognition.recordExperience()` which calls `experienceService.ingest()` — this path DOES attempt storage, but hits Bug 1.

### Bug 3: Node Duplication

Multiple cognitive nodes appear duplicated (2x attachment-style, 2x need-satisfaction per tier, 2x formative experiences, 2x entity nodes). This suggests the seeding pipeline runs twice — possibly from both cache restoration and fresh seeding.

Evidence: Two `beliefs-clara-bellingham` subgraphs with 22 and 16 nodes respectively.

## What a Character Knows After Context Clear

### WOULD know (from MindMap + seeded memories):
- Drive intensities (baseline, not adapted from runtime)
- Personality facets and attachment style
- Need satisfaction levels (baseline)
- Relational schemas (static seeded relationships)
- Formation memories / backstory episodes
- CAPS behavioral profile (attractor weights)
- Drive profiles and competition insights (LLM-generated at startup)

### Would NOT know:
- What happened this session (no runtime experience persisted usefully)
- Who said what (no dialogue transcript stored)
- What room they're in (no location tracking persisted)
- Who else is present (no co-presence tracking persisted)
- What they were doing (no action history persisted)
- Sub-thoughts about events (no sub-thought graduation from runtime)
- Emotional reactions to events (computed per-tick, discarded)
- Narrator summaries (broadcast via WebSocket, never stored)
- Tiered observation summaries (computed every tick, discarded)
- Mood state changes (tracked in-memory, not persisted to MindMap)
- Goal progress (tracked but not surfaced for context reset)

## Gap Validation

| Gap from Research Doc | Empirical Status | Notes |
|---|---|---|
| Experience recording too thin | CONFIRMED + WORSE | Domain mismatch means recall never works |
| Per-prompt rendering not persisted | CONFIRMED | Computed and discarded every tick |
| Narrator summaries not stored | CONFIRMED | Broadcast via WebSocket only |
| No transcript persistence | CONFIRMED | Full observation text discarded |
| No content condensation phase | CONFIRMED | CommunitySummaryPhase skeleton only |
| No condensation chain edges | CONFIRMED | No `condensed-from` edge type |
| No persistent entity index | CONFIRMED | Per-run accumulation only |
| No context reset injection | CONFIRMED | New capability needed |
| LevelEvent entity-blind | CONFIRMED | No speaker/room/entity fields |

### NEW findings not in research doc:
- **Domain mismatch bug** — higher severity than any listed gap; fix before Phase 2
- **Scripted mode null memory** — not mentioned; needs experienceService wiring
- **Node duplication** — seeding/cache interaction creates duplicate subgraphs

## Implications for Phase 2

1. **Fix the domain mismatch first** — this is a prerequisite for everything else. Either:
   - Change `AgentExperienceService.MANOR_DOMAIN` to `ExperienceEvents.DOMAIN`, or
   - Add a domain-agnostic recall path

2. **Wire experienceService in scripted mode** — pass the real service, not null

3. **Then persist what's already computed** — the research doc's Phase 2 plan is correct:
   - Store per-prompt tiered rendering output
   - Store narrator summaries
   - Enrich experience recording with entity references

4. **Fix node duplication** — investigate cache restoration + fresh seeding interaction

## Files

| What | Path |
|------|------|
| Pre-scenario global snapshot | `era6-memory-snapshot/post-seed-global.json` |
| Clara detailed snapshot | `era6-memory-snapshot/post-seed-clara.json` |
| This document | `era6-memory-snapshot/FINDINGS.md` |
| Snapshot endpoint (temporary) | `web/MemorySnapshotResource.java` |
