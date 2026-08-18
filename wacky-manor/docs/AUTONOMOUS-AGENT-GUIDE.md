# How to Build an Autonomous CaseHub Agent

Wacky-manor as worked example. Read the manor code to understand the pattern — this guide maps the pattern, not the cartoon specifics.

Prerequisites: a Quarkus application with `casehub-eidos`, `casehub-neocortex-memory`, and `casehub-engine-api` dependencies.

## The Pattern

An autonomous CaseHub agent needs five things:

1. **An Eidos descriptor** — personality, capabilities, goals. Registered in `AgentRegistry`.
2. **An observation boundary** — the agent sees the world through structured observations, not raw world state.
3. **A memory stack** — experience recording, salience-scored recall, reflection, relationship tracking.
4. **A goal/plan lifecycle** — goal formation from reflection, structured plans per goal, revision on failure.
5. **A tick loop** — the orchestrator that drives observe → act → record → reflect each cycle.

## 1. Register the Agent

Define the agent's identity in Eidos. The manor uses YAML descriptors loaded at startup.

```yaml
# What matters: id, personality (disposition axes), capabilities (with tags), goals
id: your-agent-id
tenancy-id: your-tenant
personality:
  disposition:
    social-orientation: collaborative    # or competitive, accommodating
    risk-appetite: cautious              # or bold, moderate
    autonomy: directed                   # or autonomous, reactive
```

Register via `AgentRegistry`. The descriptor drives personality-weighted retrieval and disposition signal recording — axes declared here determine which signals accumulate.

## 2. Build the Observation

`ObservationBuilder` in the manor produces a structured prompt from world state + cognitive state. The key sections any agent needs:

| Section | What to include | Why |
|---|---|---|
| Environment | What the agent can perceive right now | Grounds decisions in reality |
| Goals | Active `AgentGoal` instances | Drives intentional behaviour |
| Plans | Per-goal step lists | Provides tactical structure |
| Past experience | Salience-scored memories | Informed decisions from history |
| Insights | Reflection outputs | Higher-order patterns the agent noticed |
| Relationships | Per-entity interaction history | Social context for multi-agent scenarios |

The observation boundary is the only coupling between the cognitive stack and a specific world. A different world (not a manor) implements a different `buildObservation()` — the cognitive components remain identical.

## 3. Wire the Memory Stack

Three classes handle the memory lifecycle:

**`AgentExperienceService`** — the hub. Constructed with:
- `ExperienceRecorder` (platform CDI bean) — stores raw experience events
- `CaseMemoryStore` (platform CDI bean) — queries memories with salience scoring
- `ReflectionSynthesizer` (implement this) — LLM call that produces insights from raw memories
- `ManorReflectionTrigger` (implement this) — decides when to reflect (count threshold + importance threshold)

Minimum wiring:
```java
var experienceService = new AgentExperienceService(
    experienceRecorder, caseMemoryStore, tenantId,
    reflectionSynthesizer, reflectionTrigger,
    true,  // reflectionEnabled
    true,  // decayEnabled
    7, 0.2, 15, 20  // decay and recall config
);
```

Call `ingest()` after every agent action. Call `recall()` before building observations.

**Reflection synthesizer** — implement `ReflectionSynthesizer`. One LLM call that takes recent memories and produces insight strings. The manor's `ManorReflectionSynthesizer` prompts: "Given these recent experiences, what patterns or insights do you notice?"

**Reflection trigger** — implement with two thresholds: minimum unreflected memories and cumulative importance. The manor's `ManorReflectionTrigger` tracks both per-agent.

## 4. Wire Goals and Plans

Two evaluators, four strategy implementations:

**Goal lifecycle:**
- Implement `GoalFormationStrategy` — LLM call that proposes goals from context + insights
- Implement `GoalRevisionStrategy` — LLM call that revises/completes/abandons based on outcomes
- Construct `ManorGoalEvaluator` with both strategies + `AgentRegistry` + `CaseMemoryStore`

**Plan lifecycle:**
- Implement `PlanFormationStrategy` (manor-local) — LLM call that decomposes a goal into steps
- Implement `PlanRevisionStrategy` (platform SPI) — LLM call that adapts steps on failure
- Construct `ManorPlanEvaluator` with both strategies + `CaseMemoryStore`

Goals form during reflection. Plans form when goals form. Plans revise on two triggers: action failure (immediate) and reflection (deliberative).

## 5. Wire Trust and Personality

Three components, all optional but recommended for multi-agent scenarios:

**Trust** — implement `AgentTrustProvider`. The manor's `ManorTrustProvider` uses an accumulator: `recordPositive()` / `recordNegative()` called after targeted actions (GIVE vs STEAL). `currentTrustScore(agentId)` returns 0.0–1.0.

**Disposition recording** — construct `ManorDispositionRecorder` with the platform's `BehavioralSignalStore` and `DispositionSignalStore` (both CDI beans). Call `record(agentId, actionType, result)` after action resolution. Map action types to disposition axes appropriate for the domain.

**Personality evolution** — construct `ManorPersonalityEvolution` with `DispositionSignalStore`. Call `checkAndEvolve(agentId, currentTick)` after each ingest. Checks at configurable intervals and decays signal counts.

## 6. Build the Tick Loop

The orchestrator ties everything together. Each tick:

```
for each active agent:
    memories   = experienceService.recall(agentId, limit)
    memories   = PersonalityWeightedRetrieval.reweight(memories, weights, now)
    reflections = experienceService.recallReflections(agentId, 5)
    relationships = experienceService.recallRelationships(agentId, otherId, 3)
    observation = buildObservation(agent, world, goals, memories, reflections, relationships)

    response   = llm.invoke(systemPrompt, observation)
    result     = actionResolver.resolve(agent, response.action(), world)

    dispositionRecorder.record(agentId, actionType, result)
    trustProvider.recordSignal(agentId, actionType, targetAgentId)
    experienceService.ingest(agentId, room, description, thinking, importance, targetAgentId, tick)
    personalityEvolution.checkAndEvolve(agentId, tick)
```

The reflection → goal → plan cascade fires inside `ingest()` when the reflection trigger activates. No explicit orchestration needed — `AgentExperienceService` manages the cascade.

## What the Manor Proves

The cognitive stack is not game-specific. The same components — memory, reflection, goals, plans, trust, personality — work for any autonomous agent. The observation boundary is the only domain-specific piece.

A real-world CaseHub agent with MCP tools, internet access, and task execution would use the same `AgentExperienceService`, the same `GoalFormationStrategy` SPI, the same reflection pipeline. The observation would describe available tools and task state instead of rooms and objects. The action resolver would execute tool calls instead of moving between rooms.
