## D1: Three-layer architecture for emotional persistence

**Choice:** Multi-channel reinforcement — thinking-field echo (L1) + dynamic personality drives (L2) + emotional proprioception (L3)
**Alternatives:**
- Instruction-only — zero engine changes, but single-channel reinforcement may be insufficient
- Instruction + drives only — no proprioceptive feedback loop between output and drive state
**Rationale:** Presentational patterns self-reinforce through multi-channel echo (output → thinking → observation). Emotional dispositions need synthetic multi-channel reinforcement to match. Three layers create three independent feedback channels.
**Trade-offs:** More moving parts, LLM classifier adds latency/cost per turn
**Sources:** R3g full validation transcript (pareback-r3g-full-generic-20261004), TAXONOMY-ABLATION-FINDINGS.md §Design Principles 13-15
**Exploration:** quick
**Status:** captured

## D2: Unified orchestrator for personality drives

**Choice:** Extend DriveOrchestrator to handle personality drives via new PersonalityDriveEvaluator SPI alongside existing DriveSource
**Alternatives:**
- Separate PersonalityDriveState system — focused but creates parallel drive systems
- Derive from CognitiveDerivationEngine — elegant but personality drives aren't purely derived from cognitive state
**Rationale:** Single orchestrator, consistent surfacing, personality drives as first-class dynamic signals
**Trade-offs:** DriveOrchestrator grows in responsibility; personality drives use string-typed axes vs enum-typed cognitive axes
**Sources:** DriveOrchestrator.java, DriveSource.java, DriveAxis.java (casehub-neocortex-cognition-api)
**Exploration:** quick
**Depends on:** D1 (three-layer architecture)
**Status:** captured

## D3: LLM classifier for emotional proprioception

**Choice:** Lightweight LLM call (Haiku) per turn to classify emotional tone of character output against their declared drive profile
**Alternatives:**
- Keyword heuristic — fast/free but brittle, misses nuanced expression
- Self-reported in thinking — zero extra calls but risks R3c satisficing trap
**Rationale:** Proprioception must be computed externally (not self-reported) to avoid satisficing. LLM classifier captures nuance that keywords miss.
**Trade-offs:** Adds latency and API cost per character per turn
**Sources:** R3c findings (structured fields create satisficing shortcuts), TAXONOMY-ABLATION-FINDINGS.md §Design Principle 9
**Exploration:** quick
**Depends on:** D1 (three-layer architecture)
**Status:** captured

## D4: SPI extensibility for future strategies

**Choice:** Both PersonalityDriveEvaluator and EmotionalProprioceptionStrategy are SPIs in casehub-neocortex-cognition-api
**Alternatives:**
- Concrete implementations only — simpler but locks in one approach
**Rationale:** User requirement — ensure future strategies can be plugged in without changing core
**Trade-offs:** API surface grows
**Sources:** Existing pattern: GoalFormationStrategy, GoalRevisionStrategy SPIs in casehub-engine-api
**Exploration:** quick
**Depends on:** D2, D3
**Status:** captured
