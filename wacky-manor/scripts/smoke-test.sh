#!/usr/bin/env bash
# Smoke test for wacky-manor scenario — exercises all capabilities in ~8 ticks.
#
# Compressed intervals force every subsystem to fire within a short run:
#   - Consolidation at tick 5 (default 50)
#   - Reflection after 2 unreflected memories (default 5)
#   - Goal formation cooldown 2 ticks (default 10)
#   - Disposition evolution every 3 ticks (default 5)
#   - Cognitive snapshots every 3 ticks (default 0/disabled)
#   - One character in external classification mode
#
# Usage: ./smoke-test.sh [port]
# Requires: server NOT already running on the port

set -euo pipefail

PORT="${1:-8180}"
PROFILE="generic"
TURNS=15
CHARS="penelope-pitstop,hooded-claw,ant-hill-mob,dick-dastardly,peter-perfect"
EXTERNAL_CHAR="dick-dastardly"
MODULE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
AUDIT_LOG="$MODULE_DIR/docs/eval/classification-audit.jsonl"
SNAPSHOT_LOG="$MODULE_DIR/docs/eval/cognitive-snapshots-smoke.jsonl"
DEGREE_TRACE="$MODULE_DIR/docs/eval/degree-trace-smoke.jsonl"

cleanup() {
    if [[ -n "${SERVER_PID:-}" ]]; then
        kill "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
    fi
    rm -f "$AUDIT_LOG" "$SNAPSHOT_LOG" "$DEGREE_TRACE"
}
trap cleanup EXIT

echo "=== Smoke test: wacky-manor scenario ==="
echo "  Port: $PORT | Ticks: $TURNS | Characters: $CHARS"
echo "  External: $EXTERNAL_CHAR"
echo ""

# Clean prior artifacts
rm -f "$AUDIT_LOG" "$SNAPSHOT_LOG" "$DEGREE_TRACE"

# Start server with compressed intervals
echo "[1/6] Starting server..."
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f "$MODULE_DIR/pom.xml" quarkus:dev \
    -Dquarkus.http.port="$PORT" \
    -Dquarkus.profile=demo \
    -Dmanor.scenario.profile="$PROFILE" \
    -Dmanor.scenario.mode=autonomous \
    -Dmanor.scenario.max-turns="$TURNS" \
    -Dmanor.scenario.active-characters="$CHARS" \
    -Dmanor.consolidation.interval-ticks=8 \
    -Dmanor.consolidation.enabled=true \
    -Dmanor.reflection.max-unreflected=2 \
    -Dmanor.reflection.importance-threshold=1.0 \
    -Dmanor.goal.cooldown-ticks=2 \
    -Dmanor.disposition.evolution-check-interval=3 \
    -Dmanor.cognitive.snapshot-interval-ticks=3 \
    -Dmanor.classification.audit-enabled=true \
    -Dmanor.classification.external-characters="$EXTERNAL_CHAR" \
    -Dmanor.narrator.event-threshold=3 \
    -Dmanor.observation.verbatim-threshold=4 \
    -Dmanor.observation.grouped-threshold=6 \
    -Dquarkus.log.file.enable=true \
    -Dquarkus.log.file.path=/tmp/wacky-manor-smoke.log \
    -s "$MODULE_DIR/../.mvn/slot-settings.xml" \
    > /dev/null 2>&1 &
SERVER_PID=$!

# Wait for server ready
echo "[2/6] Waiting for server..."
for i in $(seq 1 60); do
    resp=$(curl -4 -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$PORT/manor/events" 2>/dev/null || echo "000")
    if [[ "$resp" == "404" || "$resp" == "200" ]]; then
        echo "  Ready after ${i}s"
        break
    fi
    if [[ "$resp" == "500" ]]; then
        echo "  FAIL: Server returned 500 — startup error"
        curl -4 -s "http://127.0.0.1:$PORT/manor/events" 2>/dev/null | \
            python3 -c "import sys,re; html=sys.stdin.read(); [print('  ' + l.strip()) for l in re.sub(r'<[^>]+>','',html).split('\n') if 'Failed' in l or 'Exception' in l][:5]" 2>/dev/null
        exit 1
    fi
    sleep 1
done

# Start scenario
echo "[3/6] Starting scenario..."
start_resp=$(curl -4 -s -X POST "http://127.0.0.1:$PORT/manor/start")
if ! echo "$start_resp" | grep -q '"started"'; then
    echo "  FAIL: Could not start scenario: $start_resp"
    exit 1
fi
echo "  $start_resp"

# Wait for completion with timeout
echo "[4/6] Running scenario (timeout 10 min)..."
DEADLINE=$((SECONDS + 600))
last_events=0
stall_count=0
while [[ $SECONDS -lt $DEADLINE ]]; do
    sleep 10
    data=$(curl -4 -s "http://127.0.0.1:$PORT/manor/events" 2>/dev/null)
    events=$(echo "$data" | python3 -c "import sys,json; d=json.loads(sys.stdin.read()); print(d.get('events', []).__len__())" 2>/dev/null || echo "0")
    complete=$(echo "$data" | python3 -c "import sys,json; d=json.loads(sys.stdin.read()); print(d.get('complete', False))" 2>/dev/null || echo "False")

    echo "  Events: $events | Complete: $complete"

    if [[ "$complete" == "True" ]]; then
        echo "  Scenario completed"
        break
    fi

    # Detect stall — no progress for 90 seconds
    if [[ "$events" == "$last_events" ]]; then
        stall_count=$((stall_count + 1))
        if [[ $stall_count -ge 9 ]]; then
            echo "  FAIL: Scenario stalled at $events events for 90s"
            exit 1
        fi
    else
        stall_count=0
    fi
    last_events=$events
done

if [[ $SECONDS -ge $DEADLINE ]]; then
    echo "  FAIL: Scenario did not complete within 10 minutes"
    exit 1
fi

# Assertions
echo "[5/6] Checking assertions..."
PASS=0
FAIL=0

check() {
    local desc="$1"
    local cond="$2"
    if eval "$cond"; then
        echo "  PASS: $desc"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $desc"
        FAIL=$((FAIL + 1))
    fi
}

# Events exist
event_count=$(curl -4 -s "http://127.0.0.1:$PORT/manor/events" 2>/dev/null | \
    python3 -c "import sys,json; print(len(json.loads(sys.stdin.read()).get('events',[])))" 2>/dev/null)
check "Events produced (got $event_count)" "[[ $event_count -gt 20 ]]"

# Scenario completed normally
completion=$(curl -4 -s "http://127.0.0.1:$PORT/manor/events" 2>/dev/null | \
    python3 -c "import sys,json; print(json.loads(sys.stdin.read()).get('reason',''))" 2>/dev/null)
check "Completed with reason DAWN (got $completion)" "[[ '$completion' == 'DAWN' ]]"

# Audit log exists and has entries
if [[ -f "$AUDIT_LOG" ]]; then
    audit_count=$(wc -l < "$AUDIT_LOG" | tr -d ' ')
    check "Audit log has entries (got $audit_count)" "[[ $audit_count -gt 0 ]]"

    # Both modes present
    inline_count=$(grep -c '"inline"' "$AUDIT_LOG" 2>/dev/null || echo "0")
    external_count=$(grep -c '"external"' "$AUDIT_LOG" 2>/dev/null || echo "0")
    check "Inline classifications present (got $inline_count)" "[[ $inline_count -gt 0 ]]"
    check "External classifications present (got $external_count)" "[[ $external_count -gt 0 ]]"

    # Source ranges in inline entries
    source_count=$(grep '"inline"' "$AUDIT_LOG" | grep -c '"source"' 2>/dev/null || echo "0")
    check "Source ranges in inline entries (got $source_count)" "[[ $source_count -gt 0 ]]"
else
    echo "  FAIL: Audit log not found at $AUDIT_LOG"
    FAIL=$((FAIL + 4))
fi

# Bias validation endpoint responds
bias_resp=$(curl -4 -s -X POST "http://127.0.0.1:$PORT/manor/bias-validation" 2>/dev/null)
bias_status=$(echo "$bias_resp" | python3 -c "import sys,json; print(json.loads(sys.stdin.read()).get('status',''))" 2>/dev/null || echo "error")
check "Bias validation endpoint works (status=$bias_status)" "[[ '$bias_status' == 'complete' || '$bias_status' == 'no_inline_entries' ]]"

# Classification mode endpoint responds
mode_resp=$(curl -4 -s "http://127.0.0.1:$PORT/manor/classification-mode" 2>/dev/null)
check "Classification mode endpoint works" "echo '$mode_resp' | grep -q 'externalCharacters'"

echo ""
echo "[6/6] Results: $PASS passed, $FAIL failed"
if [[ $FAIL -gt 0 ]]; then
    exit 1
fi
echo "=== Smoke test PASSED ==="
