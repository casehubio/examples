# Agent Gate — Strategy-Based Rate Limiting

**Issue:** casehubio/examples#30
**Date:** 2026-08-11
**Status:** Draft

## Problem

Rate limiting logic is scattered across three codebases:

- **platform `agent-gate`** — token bucket + concurrency semaphore, hardcoded in `GatedAgentProvider`
- **wacky-manor** — local `GatedAgentProvider` with concurrency-only semaphore (duplicate)
- **trellis** — sliding window counter baked into `ActionService`

All three gate the same kind of resource (LLM calls or autonomous actions) but use different patterns, different config, and can't be composed.

## Goal

Consolidate all rate limiting strategies into the platform `agent-gate` module with a consistent, composable design. Both wacky-manor and trellis migrate to use the shared implementations.

## Architecture

### AdmissionStrategy interface

```java
public interface AdmissionStrategy {

    enum Scope { INVOCATION, SESSION }

    Scope scope();
    boolean tryAcquire(Duration timeout) throws InterruptedException;
    void release();
    void rollback();
}
```

**Scope** partitions when a strategy participates:

| Scope | `invoke()` | `openSession()` | `query()` | `close()` |
|---|---|---|---|---|
| `INVOCATION` | acquire + release | acquire + release | acquire (rate-limit per query) | — |
| `SESSION` | acquire + release | acquire | — | release |

`INVOCATION`-scoped strategies gate each discrete call. `SESSION`-scoped strategies gate session lifetime — acquired once at open, released at close, not re-acquired on each `query()`.

**release() vs rollback():**

| Strategy | `release()` (operation completed) | `rollback()` (subsequent strategy failed) |
|---|---|---|
| `ConcurrencyStrategy` | Release permit | Release permit |
| `TokenBucketStrategy` | No-op (token consumed) | Return token to bucket |
| `SlidingWindowStrategy` | No-op (admission counted) | Remove recorded timestamp |

When acquiring strategies in sequence, if strategy N fails, strategies 0..N-1 are rolled back (not released). On normal completion, all strategies are released.

### Strategy implementations

| Strategy | Scope | Extracted from | Thread safety |
|---|---|---|---|
| `ConcurrencyStrategy` | `SESSION` | platform `agent-gate` (semaphore) | `Semaphore` (inherently thread-safe) |
| `TokenBucketStrategy` | `INVOCATION` | platform `agent-gate` (`TokenBucket`) | `ReentrantLock` (existing) |
| `SlidingWindowStrategy` | `INVOCATION` | trellis `ActionService` | `ReentrantLock` (same pattern as `TokenBucket`) |

### Acquisition ordering

Strategies are acquired cheapest-to-reject first to minimise resource leakage on rejection:

1. **SlidingWindowStrategy** — pure count check, no state held on failure
2. **TokenBucketStrategy** — may block briefly for refill, rollback returns token
3. **ConcurrencyStrategy** — may block longest, holds a permit

Release and rollback happen in reverse order.

### GatedAgentProvider refactor

The existing `GatedAgentProvider` CDI decorator becomes a thin orchestrator over `List<AdmissionStrategy>`:

**`invoke()`:**
1. Partition strategies by scope — all participate (both `INVOCATION` and `SESSION` strategies gate single calls)
2. Acquire in order, sharing a single deadline (`acquireTimeout`)
3. If any acquisition fails, rollback already-acquired strategies, throw appropriate exception
4. Wrap delegate stream: on termination, release all strategies in reverse order
5. Run acquisition on worker pool via `runSubscriptionOn(Infrastructure.getDefaultWorkerPool())` to avoid blocking the Vert.x event loop

**`openSession()`:**
1. Acquire all strategies (both scopes) — this is a synchronous call from a worker thread
2. On failure, rollback already-acquired strategies
3. Return `GatedAgentSession` holding the acquired strategies

The decorator remains `@Decorator @Priority(APPLICATION)`.

### GatedAgentSession refactor

Holds two partitioned lists from the strategies:
- **Session strategies** (`SESSION` scope) — acquired at open, released at `close()`
- **Query strategies** (`INVOCATION` scope) — acquired per `query()`, released on stream completion

`close()` releases session strategies in reverse order. `query()` acquires query strategies with `queryAcquireTimeout` deadline.

### Configuration

Nested config groups under `casehub.platform.agent.gate`. A single shared deadline governs the total acquisition budget — no per-strategy timeouts.

```properties
# Concurrency — active when max > 0
casehub.platform.agent.gate.concurrency.max=5

# Token bucket — active when permits-per-second > 0
casehub.platform.agent.gate.token-bucket.permits-per-second=2.0
casehub.platform.agent.gate.token-bucket.burst-capacity=5

# Sliding window — active when max-actions > 0
casehub.platform.agent.gate.sliding-window.max-actions=10
casehub.platform.agent.gate.sliding-window.window-seconds=60

# Shared timeouts
casehub.platform.agent.gate.acquire-timeout=PT30S
casehub.platform.agent.gate.query-acquire-timeout=PT5S
```

Breaking change from the current flat keys — acceptable at pre-release stage. Each strategy group is independently optional; absent or zero values disable the strategy.

`AgentGateProperties` becomes a parent interface with nested sub-interfaces per strategy group, using SmallRye `@ConfigMapping`.

### Error mapping

| Strategy failure | Exception |
|---|---|
| Concurrency timeout | `AgentSessionLimitException` (existing) |
| Token bucket timeout | `AgentRateLimitException` (existing) |
| Sliding window exceeded | `AgentRateLimitException` (reuse — semantically equivalent) |

## Consumer migrations

### Wacky-manor

1. Add `casehub-platform-agent-gate` dependency to `wacky-manor/pom.xml`
2. Configure concurrency via `application.properties`:
   ```properties
   casehub.platform.agent.gate.concurrency.max=5
   ```
3. Delete `io.casehub.examples.manor.agent.GatedAgentProvider`
4. Remove manual wrapping in `ScenarioOrchestrator` — inject `AgentProvider` directly (CDI decorator handles gating transparently)

### Trellis

1. Add `casehub-platform-agent-gate` dependency to `trellis/sidecar/pom.xml`
2. Extract the sliding window logic from `ActionService` (lines 272-292)
3. Configure via `application.properties`:
   ```properties
   casehub.platform.agent.gate.sliding-window.max-actions=5
   casehub.platform.agent.gate.sliding-window.window-seconds=60
   ```
4. Remove `isWithinRateLimit()`, `recordAutonomousExecution()`, `pruneOldTimestamps()`, `resetRateLimit()` from `ActionService`

## Components

```
agent-gate/
  src/main/java/io/casehub/platform/agent/gate/
    AdmissionStrategy.java          — interface with Scope enum
    ConcurrencyStrategy.java        — semaphore (extracted)
    TokenBucketStrategy.java        — wraps existing TokenBucket (extracted)
    SlidingWindowStrategy.java      — extracted from trellis, ReentrantLock-based
    TokenBucket.java                — unchanged (internal)
    GatedAgentProvider.java         — refactored to use List<AdmissionStrategy>
    GatedAgentSession.java          — refactored: session vs query strategy partitioning
    AgentGateProperties.java        — refactored to nested config groups
```

## Testing

- **Unit tests per strategy:** Each strategy is independently testable (no CDI needed). Test both `release()` and `rollback()` paths.
- **Composition tests:** Verify acquisition ordering, shared-deadline budget, rollback-on-partial-failure, and scope partitioning.
- **CDI integration test:** Verify the decorator activates with config and wraps the delegate.
- **Existing tests:** Refactored to use the new config structure — same assertions, different setup.

## Scope boundaries

**In scope:** Strategy interface, three implementations, GatedAgentProvider/Session refactor, config restructure, wacky-manor migration, trellis migration.

**Out of scope:** Token-aware (TPM) limiting (#31), circuit breakers (#32), multi-model awareness (#33) — these are Phase 2 extensions that may evolve the `AdmissionStrategy` interface.
