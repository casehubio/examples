# casehub-examples — Claude Code Project Guide

## Project Type

type: java
**Stage:** pre-release

## What This Project Is

Multi-example repository for CaseHub platform modules. Each subdirectory is an independent Quarkus application demonstrating a platform capability.

**Active examples:**
- `wacky-manor/` — Multi-agent LLM demo with Wacky Races characters (current focus)
- `ledger-examples/` — Ledger usage examples
- `qhorus-examples/` — Qhorus messaging examples
- `work-examples/` — WorkItems examples

**GitHub repo:** casehubio/examples

**Fork model:** origin = personal fork (`mdproctor/examples`), upstream = blessed (`casehubio/examples`)

## Build and Test

```bash
# Build wacky-manor only
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn clean install -pl wacky-manor

# Run wacky-manor tests (standard suite)
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl wacky-manor

# Run LLM evaluation tests (requires API key, non-deterministic)
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl wacky-manor -Pllm-eval

# Run wacky-manor dev mode
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn quarkus:dev -pl wacky-manor
```

**Use `mvn` not `./mvnw`** — maven wrapper not configured on this machine.

**Never run `mvn install` or `mvn test` without `-pl <module>`.** The repo has many example modules; always target the specific one.

## Work Tracking

**Issue tracking:** enabled
**GitHub repo:** casehubio/examples

## Wacky Manor

POC spec: `wacky-manor/docs/POC-SPEC.md`
Vision: `wacky-manor/docs/VISION.md`

Phase 0–2.5b complete. Phase 2.6 next: ObservationAccumulator wiring with casehub-blocks.
