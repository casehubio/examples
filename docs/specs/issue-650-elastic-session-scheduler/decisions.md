## D1: Core resource model — elastic prompt execution permits

**Choice:** The scarce resource is concurrent prompt executions, not open sessions. Sessions are unlimited. An elastic permit pool gates prompt execution using AIMD.
**Alternatives:**
- Session slot semaphore — gates session creation. Wrong: idle sessions are free.
- Combined session+ticket — conflates concerns, causes deadlock at scale.
**Rationale:** User clarified idle CLI sessions cost nothing. Backend limit is on concurrent prompts.
**Trade-offs:** Requires removing sessionSlots semaphore. SessionRegistry interface (in agent-api) becomes session lookup only, not capacity control.
**Sources:** casehubio/platform#650, user clarification
**Exploration:** quick
**Status:** captured

## D2: Configurable ceiling

**Choice:** Ceiling from config (`casehub.platform.agent.gate.concurrency.max`, default 12). AIMD adjusts within 1..ceiling.
**Alternatives:**
- Fixed at 12 — inflexible
- Auto-discovered — startup complexity
**Rationale:** Reuses existing gate config key rather than introducing a new one (review R1-03).
**Trade-offs:** Operator must set correct ceiling.
**Sources:** casehubio/platform#650, review R1-03
**Exploration:** quick
**Status:** revised (was new config key, now reuses existing)

## D3: CDI prompt lifecycle events

**Choice:** CDI events (`PromptSubmittedEvent`, `PromptCompletedEvent` records in agent-api). Observers use `@ObservesAsync`.
**Alternatives:**
- New PromptLifecycleListener SPI — adds API surface unnecessarily when CDI events achieve the same goal
**Rationale:** CDI events are idiomatic for Quarkus. No new SPI interface needed. Review R1-04 identified duplication.
**Trade-offs:** Async observation only (no synchronous blocking). Fine for metrics/auditing.
**Sources:** review R1-04, Quarkus CDI patterns
**Exploration:** quick
**Status:** revised (was SPI, now CDI events)

## D4: AIMD elasticity via AimdAdmissionStrategy

**Choice:** `AimdAdmissionStrategy implements AdmissionStrategy` in agent-gate-core. Additive Increase (+1 after N successes), Multiplicative Decrease (halve on failure).
**Alternatives:**
- New PromptPermitPool class — duplicates existing AdmissionStrategy SPI
- Extending ConcurrencyStrategy — ConcurrencyStrategy is fixed semaphore, fundamentally different state model
- Token-aware rate limiting — addresses TPM but not concurrent request limit
**Rationale:** Reuses the existing AdmissionStrategy interface (tryAcquire/release/rollback). Review R1-01/R1-09 identified duplication risk. AIMD is proven stable.
**Trade-offs:** SessionPipeline must distinguish capacity errors (→ rollback, triggers AIMD decrease) from non-capacity errors (→ release, no decrease). The AIMD strategy itself is unaware of error types — the caller decides which method to call.
**Sources:** AdmissionStrategy.java, ConcurrencyStrategy.java, TCP congestion control, review R1-01
**Exploration:** quick
**Status:** revised (was new class, now extends existing SPI)

## D5: Two-level priority

**Choice:** Add `tryAcquirePriority(Duration)` to `AdmissionStrategy` interface. Two levels: normal + priority. Pre-release — no backward compat needed, update all existing implementations to delegate to tryAcquire.
**Alternatives:**
- Pure FIFO — can't handle urgent execution needs
- Three levels — more complexity than needed
- Numeric priority — hardest to reason about
**Rationale:** Covers consolidation preempting character ticks. Simple, predictable.
**Trade-offs:** Priority requests can starve normal ones if overused.
**Sources:** user clarification ("there needs a way to have a priority execution")
**Exploration:** quick
**Status:** captured

## D6: Session idle eviction — reaper pattern

**Choice:** `PipelineSessionReaper` adapting the `SessionLeakReaper` pattern. Timer-based sweep (configurable idle threshold) + pressure-based eviction (observes `CapacityPressureEvent`). Evicted sessions re-openable via `claude -c`.
**Alternatives:**
- New SessionEvictor class — duplicates SessionLeakReaper pattern (review R1-07)
- No eviction — idle sessions accumulate unbounded
**Rationale:** Reuses the reaper pattern already proven in agent-gate. Timer-based for baseline, pressure-based for burst.
**Trade-offs:** Two eviction triggers to reason about.
**Sources:** SessionLeakReaper.java, CapacityPressureMonitor.java, review R1-07
**Exploration:** quick
**Status:** revised (was new class, now adapts existing pattern)

## D7: API surface — executeBatch + executeSingle

**Choice:** Keep executeBatch for character ticks. Add executeSingle + executeSinglePriority for consumers managing their own concurrency (consolidation).
**Alternatives:**
- executeSingle only — pushes parallelism to all callers
- executeBatch only — doesn't fit consolidation model
**Rationale:** Different consumers have different concurrency needs.
**Trade-offs:** Two API paths, both must correctly handle permits and CDI events.
**Sources:** SessionPipeline.executeBatch(), ParallelLlmExecutor (neocortex)
**Exploration:** quick
**Status:** captured

## D8: Reuse existing infrastructure

**Choice:** No new primitive classes. Extend existing platform infrastructure:
- `AimdAdmissionStrategy` implements existing `AdmissionStrategy`
- CDI events replace proposed PromptLifecycleListener SPI
- `PipelineSessionReaper` adapts existing `SessionLeakReaper` pattern
- `SessionPipeline` implements existing `CapacitySignalSource`
**Alternatives:**
- Three composable primitives (PromptPermitPool + SessionEvictor + PromptLifecycleNotifier) — duplicates gate and capacity infrastructure
- Refactor in-place with no reuse — god class risk
**Rationale:** Review surfaced significant existing infrastructure (AdmissionStrategy, CapacitySignalSource, SessionLeakReaper, CDI events). Building parallel abstractions violates platform coherence.
**Trade-offs:** Couples to existing gate and capacity APIs. Acceptable — that's the whole point.
**Sources:** review R1-01, R1-04, R1-07, R1-09, R1-12
**Exploration:** quick
**Status:** revised (was new primitives, now reuses existing)

## D9: CapacitySignalSource integration

**Choice:** SessionPipeline implements `CapacitySignalSource`, reporting `activePrompts / currentWindow` as pressure (0.0-1.0). Feeds into existing `CapacityPressureMonitor` sweep.
**Alternatives:**
- No integration — platform pressure framework can't observe session-layer pressure
**Rationale:** Review R1-12 identified this as a missing implicit decision. The elastic pool IS a capacity signal source.
**Trade-offs:** Adds dependency on platform-api. SessionPipeline already depends on agent-api; platform-api is a natural addition.
**Sources:** CapacitySignalSource.java, CapacitySignal.java, CapacityPressureMonitor.java, review R1-12
**Exploration:** quick
**Status:** new (surfaced by review)