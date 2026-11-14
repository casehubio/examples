# CaseHub Examples

All CaseHub examples in one place. Clone this repo to browse and run
examples from every CaseHub module — no need to clone individual repos.

## Prerequisites

- **Java 21+** (26 recommended)
- **Maven 3.9+**
- **GitHub Packages access** (until CaseHub publishes to Maven Central):

  Set `GITHUB_ACTOR` to your GitHub username and `GH_PACKAGES_TOKEN` to a token
  with `read:packages`. The provided `.github/maven-settings.xml` configures both
  authentication and the repository needed to resolve parent POMs in a clean cache.

## Quick Start

```bash
git clone https://github.com/casehubio/examples.git
cd examples

# Run all Maven examples
mvn --settings .github/maven-settings.xml test

# Run a specific example in dev mode
mvn --settings .github/maven-settings.xml quarkus:dev -pl ledger-examples/order-processing

# Update to latest HEAD (optional)
./sync.sh
```

### Work example prerequisites

The Work examples use current `0.4-SNAPSHOT` artifacts, including the AI module.
With the workspace dependencies installed, prepare them in a checkout of
[casehubio/work](https://github.com/casehubio/work):

```bash
mvn -Pexamples -pl examples -am install -DskipTests
```

Then, from this repository, run `mvn -pl work-examples clean verify` to execute
the Work scenario tests on H2 and package the application. This does not require
an LLM service or credentials.

### Ledger signing prerequisites

The cloud signing examples require the unpublished `0.4-SNAPSHOT` signing adapters.
In a checkout of [casehubio/ledger](https://github.com/casehubio/ledger), install them first:

```bash
mvn -Pwith-signing -pl signing/aws-kms-quarkus,signing/azure-keyvault-quarkus,signing/gcp-kms-quarkus,signing/vault-transit-quarkus -am install
```

The PR workflow prepares these adapters from a pinned Ledger commit. Wacky Manor's
live LLM tests remain opt-in through its `llm-eval` profile.

### Evaluation harness prerequisite

Wacky Manor compiles its opt-in evaluation tests against the unpublished Eidos eval
harness. In a checkout of [casehubio/eidos](https://github.com/casehubio/eidos), prepare it with:

```bash
mvn -f eval/pom.xml install -DskipTests
```

The PR workflow also prepares this harness from a pinned source commit. Its live LLM
evaluations remain opt-in.

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
| [mosaic-examples](mosaic-examples/) | TypeScript | Visual block builder |
| [pages-examples](pages-examples/) | TypeScript | Dashboard and layout editor |

## Non-Maven Examples

**openclaw** (Docker Compose): Requires Docker. See
[openclaw-examples/README.md](openclaw-examples/README.md).

**blocks-ui** (TypeScript/Vite): Requires Node.js 20+. See
[mosaic-examples/README.md](mosaic-examples/README.md).

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
| mosaic-examples | [casehubio/agency-ui](https://github.com/casehubio/casehub-pages) |
| pages-examples | [casehubio/pages](https://github.com/casehubio/casehub-pages) |

Changes flow to this repo on the next successful full build.
