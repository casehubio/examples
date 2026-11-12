# Design: DomainActivation Consumption — Cross-Subgraph Affect Correlations

**Issue:** casehubio/blocks#318
**Epic:** casehubio/blocks#311 (Neocortex cognitive integration)
**Module:** blocks-core

## Overview

Give agents self-awareness of their emotional patterns across knowledge domains. Neocortex `DomainActivation` computes which domains correlate emotionally (via DTW on time-bucketed PAD series), how mood tracks with domain activity, and which experience events shift affect. Blocks consumes these correlations and renders them as natural-language prompt sections — e.g. "your emotional patterns in work and family are strongly correlated" or "discussing project X reliably increases your arousal."

## Architecture

```
ConsolidationCompleted (CDI event, tenant-scoped)
        │
        ▼
ConsolidationMediator (@ApplicationScoped, @Observes — existing bean)
        │  stores per-tenant timestamp
        │
DomainActivationParticipant (plain class, CognitionTickParticipant)
        │  on next tick (TERMINAL phase):
        │  if consolidationMediator.lastConsolidationTimestamp(tenantId)
        │     is after lastComputedAt → recompute via DomainActivation.correlate()
        │                             → store DomainActivationSnapshot (single field)
        ▼
DomainActivationPromptSection (reads cached snapshot, renders narrative)
        │
        ▼
Agent prompt (via sectionCustomizer in SocialAvatarCognition)
```

This is a **tick participant with consolidation-staleness-check**, not a mediator and not a CDI bean. The participant is a plain class (like `CognitiveProfileParticipant`), instantiated in `SocialAvatarCognition`'s constructor. It detects new consolidation data by comparing the existing `ConsolidationMediator`'s tenant-level timestamp against its own last-computation timestamp — no separate CDI event observation needed. Heavy computation (DTW + surrogate tests) runs on the tick thread where agent identity is available and event dispatch is not blocked.

## Components

### 1. DomainActivationParticipant

**Package:** `io.casehub.blocks.agentic.social`
**Implements:** `CognitionTickParticipant`

A plain class (no CDI annotations) that participates in the cognition tick lifecycle at `CognitionPhase.TERMINAL`. Detects new consolidation data via the existing `ConsolidationMediator`'s tenant-level timestamp — no separate CDI event observation needed.

**State:**
- `lastComputedAt: Map<String, Instant>` — per agentId, timestamp of last successful computation (needed for per-agent staleness against tenant-level consolidation)
- `lastSnapshot: @Nullable DomainActivationSnapshot` — single field overwritten each tick (consistent with `CognitiveProfileParticipant`'s `lastEntityKnowledge` pattern — tick and prompt assembly run synchronously, so the field always holds the current agent's data when the section customizer reads it)

**tick(CognitionTickContext context):**
1. If `!config.domainActivationEnabled()` → `lastSnapshot = null`; return (feature kill switch, consistent with `CognitiveProfileParticipant`'s `entityKnowledgeEnabled` check)
2. Check staleness:
   ```java
   if (consolidationMediator != null) {
       var ts = consolidationMediator.lastConsolidationTimestamp(context.tenantId());
       if (ts == null || !ts.isAfter(lastComputedAt.getOrDefault(context.agentId(), Instant.EPOCH))) {
           return; // no new consolidation data — use cached
       }
   }
   // else: no mediator — skip staleness check, always compute
   ```
3. List all subgraphs via `MindMapStore.listSubgraphs(context.tenantId())`, filter to cognitive type: `sg.type().equals(SubgraphTypes.COGNITIVE)`. Cache the `MindMapSubgraph` records — their `name()` field provides human-readable domain names for prompt rendering.
4. For each subgraph, verify it has at least one entity via `mindMapStore.search(MindMapQuery.of(tenantId, 1).withSubgraphId(sg.id()))`. Filter subgraphs where the result is empty.
5. If fewer than 2 subgraphs survive filtering → `lastSnapshot = null`; return
6. Execute pairwise cascade (see algorithm below)
7. Set `lastSnapshot` to the computed `DomainActivationSnapshot`, set `lastComputedAt.put(context.agentId(), Instant.now())`

**Dependencies (constructor-injected, raw types):**
- `DomainActivation` — the neocortex service
- `MindMapStore` — for subgraph listing and entity checks
- `@Nullable ConsolidationMediator` — for staleness detection (nullable for test scenarios)
- `CognitionConfig` — for feature flag check in tick

**ConsolidationMediator addition:** Add one new public method to the existing `ConsolidationMediator`:

```java
public @Nullable Instant lastConsolidationTimestamp(String tenantId) {
    var snapshot = snapshots.get(tenantId);
    return snapshot != null ? snapshot.timestamp : null;
}
```

Read-only — does not affect the drain mechanism used by `ConsolidationPromptSection`.

**Agent identity:** `CognitionTickContext` provides `agentId` (String) and `tenantId`. Construct `PrincipalId.agent(context.agentId())` for the `DomainActivationQuery`.

**Graceful degradation:** Before the first `ConsolidationCompleted` fires, `ConsolidationMediator.lastConsolidationTimestamp()` returns null and `lastSnapshot` is null. The prompt section simply won't appear — consistent with how other cognitive subsystems handle cold start.

### 2. Pairwise Cascade Algorithm

`DomainActivation.correlate()` takes `DomainActivationQuery` with a `Set<String> subgraphIds` and computes pairwise DTW on PAD time series. The `contextDomains` parameter controls whether mood/experience correlation is included. The API returns `Optional.empty()` if any queried subgraph has no entities or no affect memories — to avoid this batch-failure mode, all queries use exactly 2 subgraphs (see deferred issue below).

**Time window:** Computed before the cascade begins:
```java
var from = Instant.now().minus(Duration.ofDays(7));
var to = Instant.now();
```
Bucket duration defaults to `Duration.ofHours(24)` (DomainActivationQuery default).

**Step 1 — Pairwise DTW scan (cheap pass):**
For each unique pair of cognitive subgraphs, call `correlate()` with just those 2 IDs and no context domains (cheap — DTW only):
```java
var principal = PrincipalId.agent(context.agentId());
var pairCorrelations = new LinkedHashMap<DomainPair, DomainCorrelation>();
var domainSignals = new LinkedHashMap<String, DomainSignal>();

for (int i = 0; i < subgraphs.size(); i++) {
    for (int j = i + 1; j < subgraphs.size(); j++) {
        var query = DomainActivationQuery.between(principal, tenantId,
                        subgraphs.get(i).id(), subgraphs.get(j).id())
                    .withFrom(from).withTo(to);
        domainActivation.correlate(query).ifPresent(result -> {
            pairCorrelations.putAll(result.correlations());
            domainSignals.putAll(result.domains());
        });
        // Pairs returning Optional.empty() (missing data) are silently skipped
    }
}
```

Note: `DomainActivationQuery.between(PrincipalId, String, String, String)` takes exactly two subgraph ID strings, not a Set. The record constructor accepts `Set<String> subgraphIds` for N-way queries, but pairwise calls avoid the batch-failure mode.

**Step 2 — Filter, sort, and cap:**
```java
var strongPairs = pairCorrelations.entrySet().stream()
    .filter(e -> e.getValue().strength().ordinal() <= CorrelationStrength.MODERATE.ordinal())
    .sorted(Comparator.comparingDouble(e -> -e.getValue().dtwSimilarity()))
    .limit(5)
    .map(Map.Entry::getKey)
    .toList();
```

Cap at 5 pairs (matching the prompt section rendering cap) to avoid computing expensive context correlations whose results would be discarded.

**Step 3 — Context correlation (expensive pass, top 5 pairs only):**
For each surviving `DomainPair`, call `correlate()` with full `contextDomains`:
```java
var contextDomains = Set.of(MoodEvents.DOMAIN, ExperienceEvents.DOMAIN);
var moodCorrelations = new LinkedHashMap<DomainPair, Map<String, DomainCorrelation>>();
var experienceImpacts = new LinkedHashMap<DomainPair, Map<String, EventImpact>>();

for (var pair : strongPairs) {
    var detailQuery = DomainActivationQuery.between(principal, tenantId,
                          pair.subgraphIdA(), pair.subgraphIdB())
                      .withContextDomains(contextDomains)
                      .withEventWindow(Duration.ofHours(2))
                      .withFrom(from).withTo(to);
    domainActivation.correlate(detailQuery).ifPresent(detail -> {
        // Transpose: API returns Map<MemoryDomain, Map<String, X>> (domain → subgraphId → value)
        // Snapshot stores Map<DomainPair, Map<String, X>> (pair → subgraphId → value)
        var moodCorrs = detail.contextCorrelations().get(MoodEvents.DOMAIN);
        if (moodCorrs != null && !moodCorrs.isEmpty()) {
            moodCorrelations.put(pair, moodCorrs);
        }
        var expImpacts = detail.eventImpacts().get(ExperienceEvents.DOMAIN);
        if (expImpacts != null && !expImpacts.isEmpty()) {
            experienceImpacts.put(pair, expImpacts);
        }
    });
}
```

**Data transposition:** `DomainActivationResult` groups context correlations as `Map<MemoryDomain, Map<String, DomainCorrelation>>` — outer key is the context domain type (mood/experience), inner key is subgraph ID. For a 2-subgraph query, the inner map has one entry per subgraph showing how that subgraph's affect correlates with mood/experience events. The snapshot transposes this to per-pair grouping by extracting the `MoodEvents.DOMAIN` and `ExperienceEvents.DOMAIN` entries separately and keying by the `DomainPair` from Step 1.

**Composite result:** The participant assembles a merged view as a `DomainActivationSnapshot`:

```java
record DomainActivationSnapshot(
    Map<DomainPair, DomainCorrelation> pairwiseCorrelations,
    Map<DomainPair, Map<String, DomainCorrelation>> moodCorrelations,
    Map<DomainPair, Map<String, EventImpact>> experienceImpacts,
    Map<String, DomainSignal> domainSignals,
    Map<String, String> subgraphNames,   // subgraphId → MindMapSubgraph.name()
    Instant computedAt
) {
    boolean hasRenderableCorrelations() {
        return pairwiseCorrelations.values().stream()
            .anyMatch(c -> c.strength().ordinal() <= CorrelationStrength.MODERATE.ordinal());
    }
}
```

`moodCorrelations` and `experienceImpacts` inner maps key by subgraph ID — each entry represents the mood-affect or experience-affect analysis for that subgraph within the pair's 2-subgraph query. The `subgraphNames` map provides human-readable domain names for prompt rendering, sourced from `MindMapSubgraph.name()` during the subgraph listing step.

**Deferred:** casehubio/neocortex#388 — make `DomainActivation.correlate()` resilient to empty subgraphs (skip them and return partial results instead of `Optional.empty()`). This would allow efficient N-way batch queries without the pairwise workaround.

### 3. DomainActivationPromptSection

**Package:** `io.casehub.blocks.agentic.social.prompt`
**Implements:** `PromptSection`

Constructed with a `DomainActivationSnapshot` (the composite result from the participant). Renders narrative text using `CorrelationStrength` qualifiers.

**`PromptContext` parameter:** Accepted but not used (consistent with `SocialComparisonPromptSection`).

**Rendering rules:**
- Only render pairs with MODERATE or STRONG correlation (WEAK/NONE filtered)
- Cap at 5 pairs maximum (most significant first, ordered by DTW similarity descending — the cascade already caps at 5 in Step 2, so this is a defensive guard)
- For each pair, render:
  - Domain names (from `subgraphNames` map — `MindMapSubgraph.name()` cached during subgraph listing)
  - Correlation strength qualifier ("strongly correlated", "moderately correlated")
  - Per-subgraph affect trajectory from `domainSignals`: for each subgraph ID in the pair, look up `domainSignals.get(subgraphId).trajectory().trend()` and render the `TrendDirection` (IMPROVING, WORSENING, STABLE). Show both subgraphs' trends, e.g. "work projects (improving), family relationships (stable)". Omit trajectory line if both are STABLE or if sample count is below 3 (`trajectory.sampleCount() < 3`).
- If mood context correlations exist for a pair (from `moodCorrelations`):
  - Render mood-domain association: "your mood is significantly associated with activity in [domain]"
  - Only include if significance test p-value < 0.05 (from `DomainCorrelation.pValue()`)
  - Note: `DomainCorrelation` provides overall DTW similarity and p-value but has no per-PAD-dimension breakdown. The rendering cannot name a specific dimension (pleasure, arousal, dominance) — only that the correlation exists and is statistically significant.
- If event impacts exist for a pair (from `experienceImpacts`):
  - `EventImpact` provides per-dimension data via `Map<PadDimension, Double> meanDelta` and `Map<PadDimension, ConfidenceInterval> confidenceInterval` — this DOES support dimension-specific rendering
  - Render per-event-type impact if confidence interval doesn't span zero
  - E.g. "collaborative events in [domain] are associated with increased pleasure" (using `meanDelta.get(PadDimension.PLEASURE)` and its confidence interval)
- Return `null` if no renderable pairs survive filtering (PromptSection contract)

**Example output:**
```
Your emotional patterns across knowledge domains:
- Your emotional states in "work projects" and "family relationships" are strongly correlated —
  when one domain activates emotionally, the other tends to follow. Your mood is significantly
  associated with activity in work projects.
- "Creative writing" and "work projects" are moderately correlated. Collaborative events in
  creative writing are associated with increased pleasure.
```

### 4. CognitionConfig Addition

Add `boolean domainActivationEnabled` as the 19th flag to `CognitionConfig`. Default to `false` in `CognitionConfig.all()` (consistent with `perspectiveComparisonEnabled` — new cognitive features default off until explicitly enabled).

The flag is checked inside the participant's `tick()` method, NOT at wiring time. Wiring is gated by dependency availability (presence pattern on the builder). This matches the existing `perspectiveComparisonEnabled` pattern: `CognitiveProfileParticipant` is unconditionally registered when `CognitiveProfile` is available, and checks `config.perspectiveComparisonEnabled()` inside its own `tick()`.

Update `CognitionConfig.with(String, boolean)` to handle `"domainActivation"` (add a case to the existing switch expression). No change needed to `without()` — it delegates to `with()` in a loop and will automatically support the new key.

### 5. SocialAvatarCognition Wiring

Add a third wiring block after the CognitiveProfileParticipant block (after line 123):

**Builder addition:**
- `Optional<DomainActivation> domainActivation` field on `SocialAvatarCognition.Builder` (default `Optional.empty()`)

**Constructor wiring (presence pattern, matching CognitiveProfileParticipant):**
```java
if (builder.domainActivation.isPresent() && builder.mindMapStore.isPresent()) {
    var domainActivationParticipant = new DomainActivationParticipant(
            builder.domainActivation.get(),
            builder.mindMapStore.get(),
            builder.consolidationMediator.orElse(null),
            CognitionConfig.all());
    core.addParticipant(CognitionPhase.TERMINAL, domainActivationParticipant);
    core.chainSectionCustomizer(sections -> {
        var result = new java.util.ArrayList<>(sections);
        var snapshot = domainActivationParticipant.lastSnapshot();
        if (snapshot != null && snapshot.hasRenderableCorrelations()) {
            result.add(new DomainActivationPromptSection(snapshot));
        }
        return result;
    });
}
```

Wiring is gated by dependency availability (`domainActivation` and `mindMapStore` must both be present), NOT by a config flag. `ConsolidationMediator` is nullable because the participant functions without it — when null, the staleness check is skipped and computation runs every tick (useful for tests and deployments without consolidation). The `CognitionConfig.all()` default has `domainActivationEnabled=false`, which is checked inside `tick()` — matching the `CognitiveProfileParticipant` pattern where `perspectiveComparisonEnabled=false` gates computation but not registration.

### 6. BlocksBeans Injection

Add `DomainActivation` to BlocksBeans via `Instance<DomainActivation>` (optional injection pattern, consistent with `CognitiveProfile` and other neocortex service wiring):

```java
@Inject Instance<DomainActivation> domainActivationInstance;
```

Add to the `socialAvatarCognition()` producer method's builder chain:
```java
.domainActivation(optionalFrom(domainActivationInstance))
```

This follows the same pattern as `.cognitiveProfile(optionalFrom(cognitiveProfileInstance))`. `DomainActivation` is `@ApplicationScoped` with `@Inject` constructor in the neocortex module, so CDI will resolve it when `casehub-neocortex-cognitive-index` is on the classpath.

### 7. Maven Dependency

Add to `blocks-core/pom.xml`:
```xml
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-neocortex-cognitive-index</artifactId>
    <scope>provided</scope>
</dependency>
```

Note: `casehub-neocortex-cognitive-index` is already a test dependency in `blocks` module and a compile dependency in `blocks-spring`. Adding it as provided to `blocks-core` follows the pattern of other neocortex API dependencies.

### 8. BlocksAutoConfiguration (Spring)

Add Spring equivalent wiring in `BlocksAutoConfiguration.socialAvatarCognition()`:
- Add `Optional<DomainActivation> domainActivation` parameter (Spring auto-wires `Optional.empty()` when the bean is absent)
- Add `.domainActivation(domainActivation)` to the builder chain

This follows the existing pattern — `socialAvatarCognition()` already accepts `Optional<CognitiveProfile>` and other optional dependencies as parameters.

### 9. Testing

**Unit tests (blocks module):**
- `DomainActivationParticipantTest`:
  - Staleness check: recomputes when `ConsolidationMediator.lastConsolidationTimestamp()` is after `lastComputedAt`
  - No recomputation when consolidation timestamp unchanged (returns cached snapshot)
  - Config kill switch: `domainActivationEnabled=false` → `lastSnapshot` set to null, no computation
  - Empty/single subgraph → `lastSnapshot` set to null
  - Pairwise cascade: pairs returning `Optional.empty()` are skipped, others proceed
  - Only MODERATE+ pairs get context correlation queries
  - Cap at 5 pairs in Step 2 (sort by DTW similarity)
  - Multiple agents share staleness check by tenant; `lastSnapshot` holds current agent's data (sync tick/render)
  - Null ConsolidationMediator → staleness check skipped, always computes (test-friendly)
- `DomainActivationPromptSectionTest`:
  - Renders MODERATE and STRONG pairs, skips WEAK/NONE
  - Caps at 5 pairs
  - Includes mood correlation text when p < 0.05
  - Includes event impact text when CI doesn't span zero
  - Returns null when no renderable pairs
  - Filters non-significant mood correlations (p >= 0.05)
  - Trajectory rendering: shows per-subgraph trends, omits when both STABLE
  - `PromptContext` parameter ignored (returns same result regardless)
- `ConsolidationMediatorTest` (addition):
  - `lastConsolidationTimestamp()` returns null before any events
  - Returns timestamp after `ConsolidationCompleted` event
  - Returns latest timestamp when multiple events for same tenant

**Integration test (examples/wacky-manor):**
- Add domain activation to an existing character with multiple cognitive subgraphs
- Verify prompt section appears after consolidation cycle

## References

- `SubgraphTypes.java` (neocortex/mindmap-api) — `COGNITIVE` constant for subgraph filtering
- `MindMapSubgraph.java` (neocortex/mindmap-api) — `name()` provides human-readable domain names
- `PrincipalId.java` (platform-api) — `PrincipalId.agent(String)` factory for agent identity
- `DomainActivation.java` (neocortex/cognitive-index) — the service being consumed
- `DomainActivationQuery.java`, `DomainActivationResult.java` — query/result types
- `EventTriggeredAnalyzer.java` — experience-affect event impact analysis
- `PadDtw.java` — DTW + surrogate significance testing
- `CorrelationStrength.java` — STRONG/MODERATE/WEAK/NONE qualifiers
- `ConsolidationMediator.java` — CDI event observation pattern (event-cache, not compute)
- `CognitiveProfileParticipant.java` — sibling tick participant at TERMINAL phase
- `SocialComparisonPromptSection.java` — sibling prompt section (numeric rendering precedent)
- `SocialAvatarCognition.java` — wiring point for participants and section customizers
- `CognitionCore.java` — tick lifecycle orchestrator
- `CognitionConfig.java` — config flag registry
- `BlocksBeans.java` — CDI producer for neocortex service injection
- `DomainActivationTest.java` (neocortex) — unit test demonstrating the API
- `CognitiveIndexWalkthroughTest.java` (neocortex examples) — integration test with therapy scenario
- casehubio/blocks#311 — parent epic
- casehubio/blocks#317 — SocialComparison sibling (pattern precedent)
