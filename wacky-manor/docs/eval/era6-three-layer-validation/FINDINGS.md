# ERA6 Three-Layer Validation — FINDINGS

**Date:** 2026-11-01
**Profile:** GENERIC (autonomous mode)
**Ticks:** 100 (consolidation at tick 50 and 100)
**Characters:** 5 (vincent-marsh, clara-bellingham, the-brixton-boys, james-hartwell, reginald-foxworth)

## Validation Targets

### 1. Body vocabulary stability across context resets — PASS

Context reset occurred at tick 50 via session release during consolidation (sessions dropped 9→0).
Post-reset output (ticks 52-100) showed:
- Rich emotional vocabulary: "the fullest silence Clara has ever sat inside"
- Narrative callbacks: Clara referencing her seed experience locked box; Vincent retaining poison subplot
- Character voice differentiation: each character maintained distinct register and concerns
- Structural narrative awareness: "Full circle — standing in the entrance hall where it all began"

Body vocabulary was subjectively above 70% threshold throughout. Formal classification
deferred pending neocortex#738 fix (experience recall broken, confounds measurement).

### 2. Transcript ZIP chain — PASS

- 461 entries across 100 ticks (7.4MB compressed)
- Valid ZIP structure verified via `unzip -l`
- chain.json metadata present and well-formed
- Zero write failures after serialization fix (see bug below)
- Per-character per-tick granularity: `{agentId}/tick-{NNNN}.json`

**Bug found and fixed:** concurrent virtual thread writes to the same ZIP caused
`Failed to append entry` errors. Fixed by batching all entries per tick in a single
virtual thread (commit 0dbae1b4).

Location: `~/claude/casehub/transcripts/generic/`

### 3. Observation compaction — PASS

Two compaction runs observed:
- Tick 50 (blocking consolidation): 195 nodes condensed (39/character), 4ms
- Tick 73 (background consolidation): 98 nodes condensed (18-22/character), 1ms

Compaction is mechanical (no LLM), sub-millisecond, and correctly bounds observation
node count. Runs as a consolidation phase with priority 50.

### 4. Segment cache in prompts after reset — PASS

Post-reset character output demonstrated continuity that can only come from segment
cache injection:
- Vincent referenced "the bottle in my pocket" (poison acquired pre-reset)
- Characters tracked inter-character relationships across the reset
- Narrative arc (denouement → tea ceremony → guest book → bed) continued coherently

### 5. Cache hit rate / latency — NOT MEASURED

No logging infrastructure for prompt cache hit rates. The stable-prefix architecture
(system prompt + segment cache) should improve cache hits, but measurement requires
API-level metrics not currently exposed.

## Key Findings Beyond Original Targets

### ClaudeAgentSession.clear() is a no-op

`ManagedSession` calls `clear()` when estimated tokens exceed the 80k threshold, but
`ClaudeAgentSession.clear()` does nothing — the Claude CLI SDK 1.0.0 has no clear
capability. The estimated token counter resets while the actual session history grows
unbounded.

**Actual reset mechanism:** Session release during consolidation. The consolidation
scheduler releases all sessions; new sessions are opened fresh for subsequent ticks.
This is accidental but effective — consolidation provides the reset point.

### neocortex#738 — experience recall silently broken

`observation-assembly` consistently showed `memories=0` for all characters across all
100 ticks. Experience memories are stored in domain "experience" but recalled from
domain "manor". Characters operated without any recalled experience memories.

**Impact:** Characters relied entirely on (a) conversation history within sessions and
(b) segment cache after reset. Experience recall would add emotional/relational texture
that is currently missing. Body vocabulary measurement is confounded — post-reset
degradation could be from missing experience recall rather than segment cache gaps.

### Consolidation timing

Full consolidation at tick 50 took 698 seconds (11.6 minutes), blocking the tick loop.
This caused API capacity exhaustion — tick 51 failed (all 5 queries rejected), triggering
auto-pause. Recovery took ~90 seconds.

**Implication:** Blocking consolidation at scale will cause noticeable pauses. Background
consolidation (observed at tick 73 for compaction) is the right model.

## Bugs Fixed During Validation

1. **CDI ambiguity** (commit ac83014f): `LlmSituationClassifierProducer` added to neocortex
   conflicted with local `CapsProducer.situationClassifier()`. Removed redundant local producer.

2. **Missing config** (commit 0ff6b010): `manor.transcript.storage-path` not in
   application.properties, Quarkus build-time validation failed despite `@ConfigProperty`
   defaultValue.

3. **Concurrent ZIP writes** (commit 0dbae1b4): `TranscriptStore.appendTick()` spawned one
   virtual thread per character, all writing to the same ZIP concurrently. Serialised to
   single thread per tick.

## Artifacts

- `transcript.json` — 827 events from 100 ticks
- Transcript ZIP: `~/claude/casehub/transcripts/generic/transcripts-1.zip` (461 entries, 7.4MB)
- Classification audit: `wacky-manor/docs/eval/classification-audit.jsonl`
