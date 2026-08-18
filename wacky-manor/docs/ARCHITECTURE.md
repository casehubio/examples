# Wacky Manor — Cognitive Architecture

How the platform's cognitive stack is wired into wacky-manor. Each section maps a platform capability to its manor-side implementation.

Not covered here: world simulation (rooms, objects, positions), UI/WebSocket layer, scripted mode. See `POC-SPEC.md` for those.

## The Tick Loop

`ScenarioOrchestrator.runAutonomousTicks()` runs the simulation. Each tick:

1. **Recall** — `AgentExperienceService.recall()` retrieves salience-scored memories; `PersonalityWeightedRetrieval.reweight()` re-orders them by disposition alignment; `recallReflections()` and `recallRelationships()` fetch insights and per-character relationship memories
2. **Observe** — `ObservationBuilder.buildObservation()` renders location, objects, characters, goals, plans, memories, insights, and relationship notes into a structured prompt
3. **Act** — LLM invocation via `AgentInvocationService`; response parsed into dialogue, aside, thinking, and action by `AgentResponse`
4. **Resolve** — `ActionResolver` validates and executes the action against `WorldState`
5. **Record** — `AgentExperienceService.ingest()` stores the experience; `ManorDispositionRecorder.record()` logs behavioural signals; `ManorTrustProvider` updates trust scores
6. **Reflect** — triggered inside `ingest()` when `ManorReflectionTrigger` fires; `ManorReflectionSynthesizer` produces insights; `ManorGoalEvaluator` forms/revises goals; `ManorPlanEvaluator` forms/revises plans; `ManorPersonalityEvolution` checks disposition signals

## Memory Stack (#42)

**Platform:** `casehub-neocortex-memory-api` — `CaseMemoryStore`, `ExperienceRecorder`, `MemoryQuery`, `MemoryOrder.SALIENCE`

| Component | Class | What it does |
|---|---|---|
| Experience ingest | `AgentExperienceService.ingest()` | Records actions as experience events with importance scoring and optional `TARGET_AGENT` metadata |
| Salience recall | `AgentExperienceService.recall()` | Queries `CaseMemoryStore` with `MemoryOrder.SALIENCE` — recency + importance weighted |
| Relationship recall | `AgentExperienceService.recallRelationships()` | Queries via `RelationshipQuery.forPair()` — returns memories between two specific agents |
| Reflection recall | `AgentExperienceService.recallReflections()` | Queries the reflection domain for synthesised insights |
| Memory decay | `AgentExperienceService.runReflection()` | After reflection, calls `store.purge(MemoryRetentionPolicy)` to remove old low-importance memories |

**Observation sections:** `pastExperienceSection()` renders recalled memories; `insightsSection()` renders reflections; `relationshipNotesSection()` renders per-character relationship context.

## Reflection (#42)

**Platform:** `casehub-neocortex-memory-api` — `ReflectionSynthesizer` SPI

| Component | Class | What it does |
|---|---|---|
| Trigger | `ManorReflectionTrigger` | Fires when unreflected memory count exceeds threshold AND cumulative importance exceeds threshold |
| Synthesizer | `ManorReflectionSynthesizer` | LLM call that produces insights from accumulated raw memories |
| Storage | `AgentExperienceService.runReflection()` | Stores insights via `ReflectionEvents.toMemoryInput()`, then triggers goal evaluation |

Reflection runs on a virtual thread spawned from `ingest()`. Non-blocking — the tick loop continues.

## Goal Lifecycle (#43)

**Platform:** `casehub-engine-api` — `GoalFormationStrategy`, `GoalRevisionStrategy` SPIs

| Component | Class | What it does |
|---|---|---|
| Formation | `ManorGoalFormationStrategy` | LLM call that proposes new goals from character context + recent insights |
| Evaluation | `ManorGoalEvaluator` | Orchestrates formation after reflection; gates by cooldown ticks and max-new-per-reflection |
| Revision | `ManorGoalRevisionStrategy` | LLM call that revises, completes, or abandons goals based on outcome counts |
| Storage | `AgentRegistry` | Goals stored on `AgentDescriptor` via `agentRegistry.updateGoals()` |

Goals form during reflection — `ManorGoalEvaluator.evaluate()` is called with insight texts from `ManorReflectionSynthesizer`. Cooldown prevents goal churn (configurable via `manor.goal.cooldown-ticks`).

## Plan Structure (#44)

**Platform:** `casehub-engine-api` — `PlanRevisionStrategy` SPI

| Component | Class | What it does |
|---|---|---|
| Data model | `AgentPlan` | Named steps with status, stored on `CharacterState.plans()` |
| Formation | `ManorPlanFormationStrategy` | LLM call that decomposes a goal into named steps |
| Evaluator | `ManorPlanEvaluator` | Orchestrates formation (on new goals) and revision (on failure or reflection) |
| Revision | `ManorPlanRevisionStrategy` | Implements `PlanRevisionStrategy` SPI — LLM call that adapts steps when actions fail |
| Observation | `ObservationBuilder.planSections()` | Renders each goal's plan as a labelled section in the prompt |

Plans form per-goal. Revision triggers on two paths: immediate (action failure via `reviseOnFailure()`) and deliberative (reflection via `reviseOnReflection()`).

## Trust and Personality (#45)

**Platform:** `casehub-neocortex-memory-api` — `AgentTrustProvider` SPI; `casehub-eidos-api` — `BehavioralSignalStore`, `DispositionSignalStore`

| Component | Class | What it does |
|---|---|---|
| Trust scoring | `ManorTrustProvider` | Implements `AgentTrustProvider`; accumulates positive/negative signals from targeted actions (STEAL → negative, GIVE → positive) |
| Disposition recording | `ManorDispositionRecorder` | Maps action outcomes to `BehavioralSignal` (SUCCESS/DECLINE) and `DispositionAxis` activations; skips trivial actions (MOVE, LOOK, WAIT) |
| Personality evolution | `ManorPersonalityEvolution` | Checks accumulated disposition signals at configurable intervals; decays counts after each check |
| Personality-weighted retrieval | Inline in `ScenarioOrchestrator` | Calls `PersonalityWeightedRetrieval.reweight()` on recalled memories before building the observation |

## Observation Builder

`ObservationBuilder` produces the structured prompt sent to each character's LLM call. The 8-argument overload (used by the autonomous tick path) renders all cognitive sections:

| Section | Source | Content |
|---|---|---|
| Current Location | `WorldState` | Room description, exits, objects, characters present |
| Inventory | `CharacterState` | Items the character carries |
| Current Thinking | `CharacterState.currentThinking()` | Free-text scratchpad from previous tick |
| Your Goals | `AgentRegistry` | Active `AgentGoal` instances with descriptions |
| Plan: \<goal\> | `CharacterState.plans()` | Per-goal step lists with status |
| Recent Activity | `ObservationService` drain | What happened since last tick |
| Past Experience | `AgentExperienceService.recall()` | Salience-scored memories, personality-weighted |
| Insights | `AgentExperienceService.recallReflections()` | Synthesised reflections |
| About \<name\> | `AgentExperienceService.recallRelationships()` | Per-character relationship memories |
| Last Action Result | `CharacterState` | Outcome of the previous action |

## Configuration

All cognitive features are gated by config properties, enabled by default:

```yaml
manor.reflection.enabled: true
manor.reflection.max-unreflected: 5
manor.reflection.importance-threshold: 3.0
manor.goal.enabled: true
manor.goal.cooldown-ticks: 10
manor.plan.enabled: true
manor.disposition.enabled: true
manor.disposition.evolution-check-interval: 5
manor.trust.enabled: true
manor.trust.positive-weight: 1.0
manor.trust.negative-weight: -2.0
manor.personality.weighted-retrieval: true
manor.memory.recall-limit: 20
manor.decay.enabled: true
manor.decay.max-age-days: 7
manor.decay.min-importance: 0.2
```
