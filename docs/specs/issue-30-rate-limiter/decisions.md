## D1: Consolidation approach

**Choice:** Extend `agent-gate` with pluggable strategies — single decorator composes N strategies
**Alternatives:**
- Separate CDI decorators per strategy — fragile ordering via `@Priority`, scattered config
- Manual wrapper chain (no CDI) — no consistency guarantee, every app wires manually
**Rationale:** Keeps the existing `agent-gate` decorator, adds sliding window alongside token bucket + semaphore, apps compose via config not code
**Trade-offs:** Slightly more complex than hardcoded strategies, but the complexity is contained in one module
**Exploration:** quick
**Status:** captured

## D2: Strategy interface design

**Choice:** Single `AdmissionStrategy` interface with `tryAcquire(Duration)` / `release()` — strategies compose as a list
**Alternatives:**
- Richer interface with `AdmissionRequest`/`AdmissionTicket` metadata — YAGNI, no current strategy uses context
- Functional composition via `UnaryOperator<Multi>` — no common release semantics, can't share deadline
**Rationale:** Clean, testable, composes naturally. Evolve to richer interface in Phase 2 (#31-33) if needed
**Trade-offs:** No per-request metadata (model, tokens, priority) — acceptable since no current strategy needs it
**Depends on:** D1 (pluggable strategies in agent-gate)
**Exploration:** quick
**Status:** captured

## D3: Refactor scope in agent-gate

**Choice:** Refactor existing token bucket and semaphore into `TokenBucketStrategy` and `ConcurrencyStrategy` implementing `AdmissionStrategy`, add `SlidingWindowStrategy` from trellis
**Alternatives:**
- Add sliding window alongside, leave existing hardcoded — two patterns coexisting, inconsistent
**Rationale:** Consistent design is the goal. Existing tests protect the refactor. GatedAgentProvider becomes a thin orchestrator over strategies.
**Trade-offs:** Touches working code, but tests validate the change
**Depends on:** D2 (AdmissionStrategy interface)
**Exploration:** quick
**Status:** captured

## D4: Configuration structure

**Choice:** Nested config groups per strategy under `casehub.platform.agent.gate.<strategy>.*`
**Alternatives:**
- Flat config keys with zero-disables convention — backwards compatible but muddled namespace
**Rationale:** Pre-release, no compat constraint. Grouped namespaces are immediately clear and scale as strategies are added. Each group maps 1:1 to an AdmissionStrategy.
**Trade-offs:** Breaking change to existing config keys — acceptable at pre-release stage
**Depends on:** D3 (refactor into strategies)
**Exploration:** quick
**Status:** captured
