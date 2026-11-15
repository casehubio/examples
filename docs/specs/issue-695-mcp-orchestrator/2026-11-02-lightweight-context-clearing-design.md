# Lightweight Context Clearing — platform#695

## Problem

`ClaudeAgentSession.clear()` currently restarts the subprocess: closes the
old `ClaudeAsyncClient`, creates a new one via `clientFactory`. This is
correct but slow — process creation, binary loading, and MCP server
re-establishment take seconds.

The platform controls all subprocess spawning (`ClaudeAgentClient.openSession()`).
No interactive user shares the stdin stream. Since we control the input,
we can send slash commands directly.

## Verified Approach

Empirical test confirms: sending `/clear` as user message content in
`--input-format stream-json` mode:

1. Is processed as a CLI slash command (not forwarded to Claude)
2. Emits a `type=conversation_reset` message
3. Assigns a new `session_id`
4. **Keeps the subprocess alive** — same PID, same MCP connections
5. Accepts new queries on the fresh context

## Design

### Change scope

Single class: `ClaudeAgentSession.clear()`. Everything above
(ManagedSession, PooledSession, SessionPool, ClearingPolicy) is unchanged.

### New clear() behavior

```
1. Guard: state must be IDLE, not CLOSED (unchanged)
2. Send sdkClient.query("/clear").messages()
3. Consume the response:
   - conversation_reset message (indicates success)
   - result message (carries new session_id)
4. Reset sessionStarted to false
5. Reset turnCounter to 0
6. Log: lightweight clear succeeded, old/new session_id, elapsed ms
```

If step 2 or 3 fails (exception, timeout, or no `conversation_reset`
in response):

```
7. Log warning: lightweight clear failed, falling back to subprocess restart
8. Execute the current factory-based clear (create new client, close old)
9. Log: fallback clear completed
```

### Observability

Each clear operation logs:
- **Lightweight success:** `Agent session cleared (lightweight) [correlationId=X, elapsed=Nms, oldSession=A, newSession=B]`
- **Fallback triggered:** `Agent session clear: lightweight failed, falling back to restart [correlationId=X, cause=...]`
- **Fallback success:** `Agent session cleared (restart) [correlationId=X, elapsed=Nms]`

`SessionMetrics` should distinguish mechanism. Current `clearTriggered()`
stays for total count; add `lightweightClearSucceeded()` and
`fallbackClearTriggered()` for mechanism breakdown.

### MessageEventMapper handling

The `conversation_reset` message type is new to the platform. During
`clear()`, it's consumed internally — never exposed to callers. If a
`conversation_reset` arrives during a normal `query()` (shouldn't happen
unless someone sends `/clear` as a prompt), `MessageEventMapper` should
skip it silently.

### sessionStarted flag

Keep `sessionStarted = true` after lightweight clear. The subprocess is
still connected — `query()` is the correct path for subsequent calls.
The empirical test confirmed post-clear queries work via the standard
`sendUserMessage()` path. Reset `turnCounter` only.

### clientFactory

The `clientFactory` field remains. Sessions created via the no-factory
constructor (test mode with `turnFactory`) skip lightweight clear and
do the no-op reset (reset `sessionStarted` and `turnCounter` only, as
they have no real subprocess).

### Thread safety

`clear()` already requires IDLE state (no active turn). The `/clear`
query is a normal turn — it transitions IDLE → ACTIVE → IDLE through
the existing state machine. No additional synchronization needed.

### Test plan

1. **Lightweight clear happy path** — mock `sdkClient.query("/clear")` to
   return a `conversation_reset` + `result` sequence. Verify no subprocess
   restart, metrics incremented.
2. **Fallback on failure** — mock `sdkClient.query("/clear")` to throw.
   Verify falls back to factory, metrics track fallback.
3. **Fallback on timeout** — mock slow response. Verify fallback triggers.
4. **Post-clear queries work** — after lightweight clear, verify
   `query()` succeeds with fresh context.
5. **Clear from test-mode session** — session with `turnFactory`, no
   `sdkClient`. Verify no-op clear (same as current behavior).
6. **Observability** — verify log messages for both paths.

## References

- ClaudeAgentSession.java:195-220 — current clear() implementation
- ClaudeAgentClient.java:282-322 — openSession() with clientFactory
- ManagedSession.java:45-57 — clearing policy triggers
- AgentSession.java:80-88 — clear() contract
- Empirical test: /tmp/test-clear-protocol.py — verified /clear behavior
- casehubio/platform#689 — predecessor issue (subprocess restart fix)
