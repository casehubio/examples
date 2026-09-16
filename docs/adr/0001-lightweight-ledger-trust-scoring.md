# 0001 — Lightweight Ledger Trust Scoring for Cognitive Agents

Date: 2026-09-16
Status: Accepted

## Context and Problem Statement

Cognitive agents need per-relationship trust scoring (how much does observer A trust subject B?) with temporal decay. The ledger module provides a mature Bayesian Beta trust model (`TrustScoreComputer`), but its documentation and primary use case centre on enterprise audit trails with JPA persistence, Merkle chains, and GDPR erasure — far heavier than what a game session or lightweight agent needs.

## Decision Drivers

* Reuse the platform's existing trust computation rather than building a parallel model
* Demonstrate lightweight ledger usage as an exemplar for cognitive agent developers
* Support both in-memory (game sessions) and durable (production) persistence via SPI
* Per-relationship trust (observer→subject) from a per-actor computation model

## Considered Options

* **Option A** — Custom event-counting heuristic in wacky-manor
* **Option B** — Full JPA ledger pipeline with database persistence
* **Option C** — TrustScoreComputer with in-memory stores and PlainLedgerEntry

## Decision Outcome

Chosen option: **Option C**, because it reuses the platform's Bayesian Beta model without imposing database overhead, and the `LedgerEntryRepository` SPI makes switching to durable persistence a config change, not a code change.

### Positive Consequences

* Full Bayesian Beta scoring with temporal decay and credibility weighting — no custom math
* `InMemoryLedgerEntryRepository` + `PlainLedgerEntry` works with no database, no Flyway, no Merkle chain
* Per-relationship trust achieved by filtering attestations by `attestorId` before scoring
* Production upgrade path: swap `selected-alternatives` to JPA implementation

### Negative Consequences / Tradeoffs

* neocortex-mindmap-intelligence gains new dependencies on ledger-api and ledger-core (provided scope)
* `LedgerEntry`/`LedgerAttestation` objects heavier than simple counters — acceptable for the computation quality
* In-memory stores require `clear()` at session boundaries to prevent unbounded growth

## Pros and Cons of the Options

### Option A — Custom event-counting heuristic

* Good, because simple to implement — count positive/negative events, compute ratio
* Good, because no new dependencies
* Bad, because duplicates what ledger already provides
* Bad, because no temporal decay, no credibility weighting, no Bayesian posterior

### Option B — Full JPA ledger pipeline

* Good, because durable trust history survives restarts
* Good, because full ledger features (Merkle chain, GDPR erasure)
* Bad, because requires database schema (Flyway migrations)
* Bad, because unnecessary overhead for single game sessions

### Option C — TrustScoreComputer with in-memory stores

* Good, because full Bayesian Beta scoring
* Good, because no database overhead
* Good, because SPI-driven — swap to JPA via config when needed
* Good, because exemplar of lightweight ledger usage for the platform
* Bad, because trust history lost on process restart (acceptable for game scope)

## Links

* casehubio/examples#65 — Trust evolution from experience
* casehubio/examples#64 — Phase C: Dynamic cognitive evolution
* Design decisions D1, D9 in specs/issue-65-trust-evolution/decisions.md
* GE-20260916-2fcb25 — Lightweight ledger trust scoring garden entry
* GE-20260429-42fb02 — Bayesian Beta trust returns 0.5 for no evidence
