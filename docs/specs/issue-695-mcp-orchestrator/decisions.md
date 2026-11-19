# Decisions — platform#695: Lightweight context clearing

## D1: Clear mechanism

**Choice:** Send `/clear` as a user message via `sdkClient.query("/clear")`
**Alternatives:**
- Subprocess restart via clientFactory — current approach, multi-second latency, MCP reconnection cost
- Raw transport sendMessage — bypasses SDK query(), couples to transport internals
- Session ID rotation — not supported by CLI (verified: session_id field is for resume, not isolation)
**Rationale:** Empirically verified: `/clear` as user message content in stream-json mode emits `conversation_reset`, assigns a new session_id, and keeps the subprocess alive. Uses the SDK's own query() API, no internal coupling.
**Trade-offs:** Requires CLI version that supports slash commands in stream-json mode (verified on current CLI). Older versions would need the factory fallback.
**Sources:** Empirical test (test-clear-protocol.py), SDK source (DefaultClaudeAsyncClient.java, StreamingTransport.java), claude-code-sdk-1.0.0-sources.jar
**Exploration:** quick
**Status:** captured

## D2: Fallback strategy

**Choice:** Try /clear first; fall back to subprocess restart via clientFactory if /clear fails or times out
**Alternatives:**
- No fallback — require /clear support, throw on failure. Simpler but breaks on older CLI.
**Rationale:** Graceful degradation. The factory restart is the current working behavior. Adding /clear as the primary path improves performance while preserving correctness on older CLIs.
**Trade-offs:** Slightly more complex clear() implementation with try-catch and fallback path. Fallback path must be tested.
**Depends on:** D1 (clear mechanism)
**Sources:** Current ClaudeAgentSession.clear() implementation
**Exploration:** quick
**Status:** captured