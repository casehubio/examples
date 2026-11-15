# Elastic Session Scheduler — Design Spec

**Issue:** casehubio/platform#650
**Branch:** issue-650-elastic-session-scheduler
**Date:** 2026-10-21

## Problem

Three consumers compete blindly for the Claude backend's concurrent prompt limit:

- `SessionPipeline`: sticky character + classify sessions, ticket-controlled
- `ParallelLlmExecutor` (neocortex consolidation): own session pool, no coordination
- Backend hard limit: concurrent prompt executions (configurable, default 12)

At tick 50+, consolidation fires and tries to run prompts. Combined demand exceeds
the backend limit. Consolidation stalls.

The current implementation has two fixed semaphores (`tickets` and `sessionSlots`)
that never adapt. `sessionSlots` conflates session existence with prompt execution —
idle sessions block other consumers. The ticket mechanism only shrinks on capacity
errors and slowly recovers 1 permit/second.

## Core Insight

**The scarce resource is concurrent prompt executions, not open sessions.** Idle CLI
sessions cost nothing — dozens can be open simultaneously. The constraint is on how
many sessions can have an active prompt in-flight at the same time. The `sessionSlots`
semaphore is removed entirely.

## Design

### 1. AimdAdmissionStrategy

A new `AdmissionStrategy` implementation in `agent-gate-core` that uses Additive
Increase, Multiplicative Decrease (AIMD) — the TCP congestion control algorithm.

```java
public class AimdAdmissionStrategy implements AdmissionStrategy {

    private final int ceiling;
    private int window;                  // guarded by lock
    private int available;               // guarded by lock — permits not currently held
    private int successCount;            // guarded by lock
    private final int increaseThreshold;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition priorityWaiters = lock.newCondition();
    private final Condition normalWaiters = lock.newCondition();
    private int priorityWaiting;         // guarded by lock — count of priority waiters
    private final Duration normalAgingTimeout; // after this, normal promotes to priority
}
```

**Synchronization:** All state is guarded by a single `ReentrantLock`. No
`Semaphore` — the lock + conditions handle permits, priority, and window
mutations atomically. This avoids the `Semaphore.reducePermits()` problem:
when `rollback()` halves the window, the lock holds while `available` is
adjusted. If more permits are outstanding than the new window, `available`
goes to 0 and new acquires block until releases bring it back up.

**AIMD parameters:**
- Ceiling: configurable (`casehub.platform.agent.gate.concurrency.max`, default 12)
- Initial window: ceiling (start optimistic)
- Increase threshold: configurable (default 5 consecutive successes before +1)
- Decrease factor: 0.5 (halve on failure)
- Minimum window: 1

**Two-phase recovery (TCP-style):**
- **Slow start:** when window < ceiling/2, double window on each threshold
  (exponential growth — fast recovery from total failure)
- **Congestion avoidance:** when window >= ceiling/2, grow by 1 on each
  threshold (linear growth — careful near capacity)

This addresses the 10x recovery problem: from window=1 to ceiling=12, slow
start doubles (1→2→4→8) requiring 4 thresholds, then congestion avoidance
adds (8→9→10→11→12) requiring 4 more. Total: ~40 prompts vs ~55 with
pure linear AIMD.

**Scope:** `INVOCATION` — acquired per-prompt, released when the prompt completes.
This differs from `ConcurrencyStrategy` which uses `SESSION` scope and is acquired
once per session open.

**Priority:** `tryAcquirePriority(Duration)` is a method on `AimdAdmissionStrategy`
directly — NOT on the `AdmissionStrategy` interface. Priority is an AIMD concern,
not a general admission concern. `SessionPipeline` references the concrete type
for priority access.

Two levels: normal (character ticks) and priority (consolidation, urgent).
Priority waiters are signalled first when permits become available.

**Starvation prevention:** Normal requests that have waited longer than
`normalAgingTimeout` (configurable, default 10s) are promoted to priority.
This bounds starvation to the aging timeout regardless of priority demand.

**Why AIMD:** Proven stable, converges to the right level, avoids oscillation.
Decades of production use in TCP congestion control.

**Why not extend ConcurrencyStrategy:** ConcurrencyStrategy is a fixed semaphore —
no growth, no shrink. AIMD requires fundamentally different state management
(mutable window, success counting, dynamic permit adjustment).

**Error classification:** `SessionPipeline` determines whether an exception is
a capacity error via `isCapacityError(Exception)` — matches `SessionCapacityException`,
`AgentRateLimitException`, `AgentSessionLimitException`, and any exception whose
message contains "429" or "rate limit". Capacity errors → `rollback()`. All
others → `release()`.

### 2. SessionPipeline Refactoring

Remove the `sessionSlots` semaphore entirely. Replace the `tickets` semaphore with
`AimdAdmissionStrategy`.

**Before:**
```
SessionPipeline
  ├── sessionSlots: Semaphore(maxSessions)     // REMOVE
  ├── tickets: Semaphore(maxConcurrency)        // REPLACE
  ├── sessions: ConcurrentHashMap               // KEEP
  └── recovery: ScheduledExecutor               // REMOVE (AIMD handles this)
```

**After:**
```
SessionPipeline
  ├── admission: AimdAdmissionStrategy          // replaces tickets
  ├── sessions: ConcurrentHashMap               // unchanged
  └── lastUsed: ConcurrentHashMap<String, Instant>  // for eviction
```

**executeBatch** — unchanged API. Internally acquires an AIMD permit before each
prompt, releases on success, rolls back on failure. Virtual threads per session.

**executeSingle** — new method for consumers that manage their own concurrency
(consolidation phases via ParallelLlmExecutor). Acquires a single AIMD permit,
executes the prompt, releases/rolls back.

```java
public <T> T executeSingle(String sessionId,
                           Function<AgentSession, T> queryFn,
                           Duration timeout) {
    if (!admission.tryAcquire(timeout)) {
        throw new AgentSessionLimitException(admission.activeCount());
    }
    try {
        T result = queryFn.apply(sessionFor(sessionId));
        admission.release();
        return result;
    } catch (Exception e) {
        if (isCapacityError(e)) {
            admission.rollback();
        } else {
            admission.release();
        }
        throw e;
    }
}

public <T> T executeSinglePriority(String sessionId,
                                    Function<AgentSession, T> queryFn,
                                    Duration timeout) {
    if (!admission.tryAcquirePriority(timeout)) {
        throw new AgentSessionLimitException(admission.activeCount());
    }
    // ... same try/catch pattern
}
```

**Blocking contract:** `queryFn` MUST block until the prompt completes. It
must not return a reactive type (`Multi`, `CompletableFuture`) without
subscribing and blocking. The permit is released on return — an early return
releases the permit before the backend work finishes, defeating concurrency
control.

**Session lifecycle unchanged:** `openSession` creates a `ManagedSession` via
`SessionLifecycleManager` with no permit cost. `releaseSession` closes the
session. `sessionFor` retrieves an existing session. No semaphore involved.

### 3. CDI Prompt Lifecycle Events

Two CDI event records in `agent-api`:

```java
public record PromptSubmittedEvent(String sessionId, String backendKey, Instant submittedAt) {}
public record PromptCompletedEvent(String sessionId, String backendKey,
                                    Instant completedAt, Duration duration,
                                    boolean success, String failureReason) {}
```

Fired by `SessionPipeline` around each prompt execution (in both `executeBatch`
and `executeSingle`). Observers use `@ObservesAsync` to avoid blocking prompt
execution.

**Consumers:** SessionMetrics (#647), future auditing, debugging. The AIMD
strategy itself does NOT depend on these events — it gets feedback directly
from `release()`/`rollback()`.

### 4. CapacitySignalSource Integration

`SessionPipeline` implements `CapacitySignalSource`:

```java
@Override
public List<CapacitySignal> observe(String actorId) {
    // Use ceiling (not dynamic window) as denominator — avoids over-reacting
    // during AIMD recovery when window is temporarily small
    double pressure = (double) admission.activeCount() / admission.ceiling();
    return List.of(new CapacitySignal(
        actorId, "prompt-concurrency", pressure, Instant.now()));
}

@Override
public List<CapacitySignal> observeOverloaded(double threshold) {
    double pressure = (double) admission.activeCount() / admission.ceiling();
    if (pressure >= threshold) {
        return List.of(new CapacitySignal(
            "pipeline", "prompt-concurrency", pressure, Instant.now()));
    }
    return List.of();
}
```

This feeds into the existing `CapacityPressureMonitor` sweep. When prompt
concurrency pressure exceeds the threshold, `CapacityPressureEvent` fires,
which can trigger session eviction (see §5).

### 5. Session Idle Eviction

**Timer-based:** A `PipelineSessionReaper` (same pattern as `SessionLeakReaper`)
scans `SessionPipeline.lastUsed` map periodically. Sessions idle longer than
a configurable threshold are released (closed). Evicted sessions can be re-opened
via `claude -c` when needed — the `SessionLifecycleManager` handles this
transparently via `acquire()`.

**Pressure-based:** Observes `CapacityPressureEvent` (from `CapacityPressureMonitor`).
When prompt concurrency pressure exceeds the threshold, evicts the longest-idle
sessions to free backend resources. This doesn't directly free AIMD permits (idle
sessions don't hold permits), but reduces backend process count for resource cleanup.

```java
public class PipelineSessionReaper {

    private final SessionPipeline pipeline;
    private final Duration idleThreshold;   // e.g. 5 minutes

    @Scheduled(every = "${casehub.agent.session.reaper.interval:60s}")
    void sweep() {
        pipeline.evictIdleSessions(idleThreshold);
    }

    void onPressure(@ObservesAsync CapacityPressureEvent event) {
        if ("prompt-concurrency".equals(event.trigger())) {
            pipeline.evictIdleSessions(Duration.ofSeconds(30));
        }
    }
}
```

### 6. ParallelLlmExecutor Changes

Remove the two execution paths. Replace with a single path that uses
`SessionPipeline.executeSinglePriority()` for each consolidation task:

```java
public List<T> execute(SessionPipeline pipeline,
                       String systemPrompt,
                       String phaseName,
                       int concurrency,
                       List<Task<T>> tasks) {
    // Open sessions through pipeline (no permit cost)
    var sessionIds = openSessions(pipeline, systemPrompt, phaseName, concurrency, tasks);

    // Execute tasks using virtual threads + priority permits
    // AIMD controls concurrency — virtual threads just provide enough threads
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var futures = tasks.stream()
            .map(task -> executor.submit(() ->
                pipeline.executeSinglePriority(
                    sessionIds.get(task), task::execute, Duration.ofSeconds(120))))
            .toList();
        var results = futures.stream().map(CompletableFuture::join).toList();
    }

    // Release sessions when phase completes
    sessionIds.values().forEach(pipeline::releaseSession);
    return results;
}
```

Consolidation uses priority permits so it doesn't starve behind character
ticks during heavy load.

### 7. PlaybookOrchestrator Changes

- Replace `maxConcurrentSessions` with a generous soft cap (default 100) as a safety net against unbounded session creation
- `SessionPipeline` constructor takes `AimdAdmissionStrategy` (or just the ceiling config)
- Character tick queries use `executeBatch` (normal priority)
- Consolidation uses `executeSinglePriority` through the same pipeline

## Module Placement

| Artifact | Module | Why |
|----------|--------|-----|
| `AimdAdmissionStrategy` | `agent-gate-core` | Implements existing `AdmissionStrategy` SPI |
| `AimdAdmissionStrategy.tryAcquirePriority()` | `agent-gate-core` | Priority is AIMD-specific, not on the SPI |
| `PromptSubmittedEvent`, `PromptCompletedEvent` | `agent-api` | CDI event records, consumed by any observer |
| `PipelineSessionReaper` | `agent-session-core` | Adapts `SessionLeakReaper` pattern for pipeline |
| `SessionPipeline` refactoring | `agent-session-core` | Existing class |
| `CapacitySignalSource` impl | `agent-session-core` | SessionPipeline implements platform-api interface |

## Testing Strategy

Build tests bottom-up — primitives first, composition second, integration last.

### Layer 1: AimdAdmissionStrategy (unit tests)

All pure in-memory, no sessions, no backend.

- **Acquire/release cycle** — basic permit flow
- **AIMD growth** — N consecutive successes → window +1 (up to ceiling)
- **AIMD shrink** — rollback → window halved (minimum 1)
- **Slow start** — window doubles (exponential growth) when below ceiling/2
- **Congestion avoidance** — window grows by 1 (linear) when at/above ceiling/2
- **AIMD convergence** — mixed success/failure → stabilizes at sustainable level
- **Ceiling enforcement** — window never exceeds configured ceiling
- **Priority ordering** — priority acquires served before normal waiters
- **Priority starvation prevention** — normal requests eventually served even with constant priority demand
- **Timeout behavior** — acquire times out when no permits available
- **Concurrent stress** — many threads acquiring/releasing, verify no permit leaks
- **Thread safety** — window adjustments under contention

### Layer 2: SessionPipeline (unit tests)

Mock `SessionLifecycleManager` and `AimdAdmissionStrategy`. Test the wiring.

- **executeBatch** — permits acquired per prompt, released on completion
- **executeSingle** — single permit lifecycle
- **executeSinglePriority** — priority permit path
- **Capacity error handling** — `SessionCapacityException` → rollback (not release)
- **Non-capacity error handling** — other exceptions → release (not rollback)
- **CDI events fired** — PromptSubmitted on acquire, PromptCompleted on release/rollback
- **Session lifecycle independent** — openSession/releaseSession don't touch permits
- **CapacitySignalSource** — pressure correctly calculated as activeCount/ceiling
- **Capacity error classification** — `isCapacityError()` correctly identifies capacity vs non-capacity exceptions
- **lastUsed tracking** — updated on every prompt execution

### Layer 3: PipelineSessionReaper (unit tests)

- **Timer sweep** — sessions idle > threshold are evicted
- **Pressure sweep** — shorter threshold on pressure event
- **Active sessions not evicted** — sessions with in-flight prompts are skipped
- **Re-openable** — evicted session ID can be re-opened via pipeline

### Layer 4: Composition (integration tests)

Test the full stack with a mock backend.

- **Character ticks + consolidation** — both compete fairly for permits
- **Priority under load** — consolidation gets permits when characters hold most
- **AIMD adapts to backend** — simulate capacity errors, verify window shrinks and recovers
- **Eviction under pressure** — pressure signal triggers idle session cleanup
- **Capacity signal reported** — CapacityPressureMonitor sees pipeline pressure

### Layer 5: Scenario run (manual)

Full wacky-manor scenario run to verify elastic scheduling works end-to-end
when consolidation fires at tick 50+. Observe that consolidation no longer stalls.

## Implementation Order

1. `AimdAdmissionStrategy` + tests (agent-gate-core) — includes `tryAcquirePriority`
3. CDI events in agent-api (`PromptSubmittedEvent`, `PromptCompletedEvent`)
4. Refactor `SessionPipeline` — remove `sessionSlots`, replace `tickets` with AIMD, add `executeSingle`/`executeSinglePriority`, implement `CapacitySignalSource`, track `lastUsed`
5. Update SessionPipeline tests
6. `PipelineSessionReaper` + tests
7. Refactor `ParallelLlmExecutor` — single path using pipeline
8. Update `PlaybookOrchestrator` wiring
9. Full test run across platform + neocortex + examples

## References

- `agent-gate-core/.../AdmissionStrategy.java` — existing SPI (tryAcquire/release/rollback)
- `agent-gate-core/.../ConcurrencyStrategy.java` — existing fixed-semaphore AdmissionStrategy
- `agent-gate-core/.../GatedAgentProviderWrapper.java` — composition pattern for strategies
- `agent-gate-core/.../AdmissionUtils.java` — acquireAll/releaseAll composition
- `agent-gate-core/.../SessionLeakReaper.java` — reaper pattern for session eviction
- `platform-api/.../CapacitySignalSource.java` — capacity pressure SPI
- `platform-api/.../CapacitySignal.java` — pressure signal record (0.0-1.0)
- `platform/.../CapacityPressureMonitor.java` — scheduled sweep firing CapacityPressureEvent
- `agent-session-core/.../SessionPipeline.java` — current implementation (refactoring target)
- `agent-session-core/.../SessionLifecycleManager.java` — session creation (bypasses gate)
- casehubio/platform#650 — issue design
- casehubio/platform#647 — SessionMetrics (consumer of CDI events)
- casehubio/platform#242 — SessionLeakReaper (pattern reuse)
- casehubio/platform#268 — Capacity Signal SPI
