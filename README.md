# CaseHub Examples

All CaseHub examples in one place. Clone this repo to browse and run
examples from every CaseHub module — no need to clone individual repos.

## Prerequisites

- **Java 21+** (26 recommended)
- **Maven 3.9+**
- **GitHub Packages access** (until CaseHub publishes to Maven Central):

  Add to `~/.m2/settings.xml`:
  ```xml
  <server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_GITHUB_TOKEN</password>
  </server>
  ```
  The token needs `read:packages` scope.

## Quick Start

```bash
git clone https://github.com/casehubio/examples.git
cd examples

# Run all Maven examples
mvn test

# Run a specific example in dev mode
mvn quarkus:dev -pl ledger-examples/order-processing

# Update to latest HEAD (optional)
./sync.sh
```

## Example Sets

| Example Set | Type | Description |
|---|---|---|
| [ledger-examples](ledger-examples/) | Quarkus | Immutable audit trails, Merkle proofs, trust scoring, GDPR compliance |
| [work-examples](work-examples/) | Quarkus | Human task lifecycle, SLA enforcement, queues, AI routing |
| [qhorus-examples](qhorus-examples/) | Quarkus | Agent communication, speech acts, normative channel layout |
| [eidos-examples](eidos-examples/) | Quarkus | Agent scenario orchestration |
| [desiredstate-examples](desiredstate-examples/) | Quarkus | Desired-state reconciliation, drift detection, pipelines |
| [neocortex-examples](neocortex-examples/) | Quarkus | RAG pipelines, case-based reasoning, text analysis |
| [openclaw-examples](openclaw-examples/) | Docker Compose | Multi-agent rule authoring scenarios |
| [blocks-ui-examples](blocks-ui-examples/) | TypeScript | Visual block builder |
| [pages-examples](pages-examples/) | TypeScript | Dashboard and layout editor |

## Non-Maven Examples

**openclaw** (Docker Compose): Requires Docker. See
[openclaw-examples/README.md](openclaw-examples/README.md).

**blocks-ui** (TypeScript/Vite): Requires Node.js 20+. See
[blocks-ui-examples/README.md](blocks-ui-examples/README.md).

**pages** (TypeScript/webpack): Requires Node.js 20+. See
[pages-examples/README.md](pages-examples/README.md).

## LLM Examples (Qhorus)

The qhorus `agent-communication` module requires a local LLM model.
It is excluded from the default build. To run it:

```bash
mvn test -Pwith-llm-examples -pl qhorus-examples
```

## About This Repo

This repository is **read-only** — it is automatically synced from the
source repos after each successful
[full build](https://github.com/casehubio/parent/actions/workflows/build-all.yml).
Examples always match a known-good build state.

To fix a bug or add an example, make the change in the source repo:

| Example Set | Source Repo |
|---|---|
| ledger-examples | [casehubio/ledger](https://github.com/casehubio/ledger) |
| work-examples | [casehubio/work](https://github.com/casehubio/work) |
| qhorus-examples | [casehubio/qhorus](https://github.com/casehubio/qhorus) |
| eidos-examples | [casehubio/eidos](https://github.com/casehubio/eidos) |
| desiredstate-examples | [casehubio/desiredstate](https://github.com/casehubio/casehub-desiredstate) |
| neocortex-examples | [casehubio/neocortex](https://github.com/casehubio/neocortex) |
| openclaw-examples | [casehubio/openclaw](https://github.com/casehubio/openclaw) |
| blocks-ui-examples | [casehubio/blocks-ui](https://github.com/casehubio/casehub-pages) |
| pages-examples | [casehubio/pages](https://github.com/casehubio/casehub-pages) |

Changes flow to this repo on the next successful full build.
