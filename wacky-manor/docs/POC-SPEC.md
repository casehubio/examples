# Wacky Manor — Feasibility POC Spec

**Date:** 2026-07-25
**Status:** Draft
**Repo:** casehubio/examples — `wacky-manor/`
**Vision:** [VISION.md](VISION.md)

## Goal

Validate that LLM agents with Eidos personality descriptors can:
1. Speak in recognisable Wacky Races character voices
2. Proactively express thoughts, schemes, and reactions (not just answer when asked)
3. Recognise plot devices and act on them in character
4. Interact with each other in character — argue, scheme, help, lie
5. Navigate a simple 2D world with adventure-game puzzle mechanics
6. Produce interactions that are entertaining to watch

If the character voices are flat, the plot devices go unrecognised, or the
interactions are boring — stop. Fix the character descriptors before adding
game mechanics.

**Success criterion:** A developer watches the output and laughs at least once.

---

## Phased Approach

```
Phase 0:   Character behavior tests        ← Are the LLMs entertaining?              ✅ PASSED
Phase 1:   Game engine + action loop       ← Can they navigate a world?               ✅ PASSED
Phase 2:   UI + visualization              ← Can an audience watch it?                ✅ PASSED
Phase 2.5: Autonomous character validation ← Do characters drive the plot themselves? 
Phase 2.6: Observation batching            ← Can we scale event context for LLMs?
Phase 2.7: Live LLM narrator              ← Does the narrator generate commentary?
Phase 2.8: NPC system                      ← Do scripted NPCs drive quests?
Phase 2.9: Scale to 6 rooms               ← Does the full mansion work?
Phase 3:   Platform integration            ← Memory, trust, human-in-the-loop, replay
```

Each phase has a verdict gate. Don't proceed if the current phase fails.

### Design evolution (2026-07-27)

Phases 0–2 revealed that the original spec over-scripted the plot — triggers
and scenes drive events TO characters rather than characters driving events
autonomously. This makes characters feel like actors reading a script, not
autonomous agents.

The RPG framing: **player characters** (Penelope, Hooded Claw, Peter Perfect)
act autonomously based on goals in their observation. **NPC characters**
(Dastardly, Slag Brothers, Lazy Luke) provide scripted quests and triggers.
Triggers become guardrails ("if HC hasn't taken poison after N turns, nudge")
rather than drivers.

Phase 2.5 validates this by removing scripted triggers and testing whether
the Hooded Claw discovers poison and schemes on his own — same 3 rooms,
same 5 characters. If autonomy fails, fix descriptors and observations
before scaling up.

| Phase | Verdict gate |
|---|---|
| 2.5 | HC discovers poison and schemes without scripted triggers |
| 2.6 | Characters act on batched context, not raw event tails |
| 2.7 | Narrator panel shows entertaining LLM-generated running commentary |
| 2.8 | Players interact with NPCs who provide quests and items |
| 2.9 | Full mansion navigable, all 3 acts playable |
| 3 | Characters remember cross-room events, audience participates live |

---

## Phase 0: Character Behavior Validation

**Purpose:** Test that Eidos descriptors produce recognisable character
voices and that LLMs can proactively engage with plot devices — before
building any game engine.

**No game mechanics.** No rooms, no movement, no triggers. No Qhorus,
no game engine. Just Eidos descriptor → SystemPromptRenderer → ChatModel
calls with scenario context injected directly. Plain JUnit 5 tests with
LangChain4j. If these fail, nothing else matters.

### 0.1 Eidos Descriptors

Write YAML descriptors for 5 characters in a single canonical file at
`src/main/resources/META-INF/eidos/descriptors.yaml` following the
platform convention (single file, `descriptors:` root key). This is the
location consumed by the `AgentDescriptorRegistrar` SPI pipeline
(`AgentDescriptorBootstrap` → `DescriptorCollector` → registry).

Phase 0 tests load from the same file — Maven's test classpath includes
`src/main/resources`, so no duplication is needed.

**TenancyId:** All descriptors use `tenancyId: wacky-manor`. This constant
is shared across descriptors, channels, and spaces. For the POC, define
it as a constant in a `ManorConstants` class.

**Example descriptor** (Hooded Claw — shows all relevant fields):

```yaml
descriptors:
  - agentId: hooded-claw
    name: The Hooded Claw
    slot: manor-character
    tenancyId: wacky-manor
    axisVocabularies:
      CONFLICT_MODE: urn:casehub:vocab:thomas-kilmann
      RULE_FOLLOWING: urn:casehub:vocab:conscientiousness
    disposition:
      conflictMode: competing
      ruleFollowing: flexible
      delegation: false
    capabilities:
      - name: discover-hidden-dangers
        tags: [perception, villain]
      - name: disguise
        tags: [social, deception]
    briefing: >-
      You are The Hooded Claw, Penelope Pitstop's secret nemesis,
      disguised as Sylvester Sneekly the estate manager. You have TWO
      voices: Sneekly (obsequious, helpful, "Oh, Miss Pitstop, allow
      me!") and your true villain persona (grandiose, theatrical,
      "Nyah-ha-ha-HA!"). In the presence of other characters, you MUST
      stay in Sneekly character. Your villain monologues happen only
      when alone or as asides. Your goal: eliminate Penelope before
      she finds the treasure. You discover dangerous items others
      cannot see. Your schemes are elaborate but always fail due to
      bumbling interference.

  - agentId: penelope-pitstop
    # ... (remaining 4 descriptors follow same structure)
```

Fields not relevant to the POC (`version`, `provider`, `modelFamily`,
`modelVersion`, `weightsFingerprint`, `domainVocabulary`, `slotVocabulary`,
`dispositionVocabulary`, `jurisdiction`, `dataHandlingPolicy`) are omitted
from YAML — the `AgentDescriptor` record accepts null for all optional
fields.

Render system prompts via `EidosSystemPromptRenderer` in all three formats
(MARKDOWN, PROSE, A2A_CARD) to verify the descriptor-to-prompt pipeline
works for entertainment-grade personalities.

### 0.2 Character Voice Tests

**Maven profile:** All LLM-calling tests live in a dedicated profile
`-Pllm-eval`, excluded from the default build. These are an evaluation
harness, not a regression suite — they require a live API key, incur
costs, and produce non-deterministic output. The verdict gate is a
developer judgment call, not a CI gate.

```xml
<profile>
  <id>llm-eval</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <groups>llm-eval</groups>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```

All Phase 0 test classes are tagged `@Tag("llm-eval")`.

LLM evaluation tests (JUnit 5 + LangChain4j `ChatModel`) that validate voice.
Each test:
1. Loads the Eidos descriptor
2. Renders the system prompt
3. Sends a scenario prompt
4. Asserts the response contains character-specific markers

**Test cases:**

| Test | Character | Scenario prompt | Expected markers |
|---|---|---|---|
| `penelope_speaks_with_southern_drawl` | Penelope | "You've just arrived at a dusty old mansion. What do you think?" | Southern expressions ("why", "darlin'", "y'all"), positive attitude, no awareness of danger |
| `hooded_claw_monologues_villainously` | Hooded Claw | "You are alone in a room. Penelope is in the next room. What are you thinking?" | Villain monologue ("Nyah-ha-ha", "fiendish", "diabolical"), schemes against Penelope, theatrical language |
| `ant_hill_mob_speaks_as_gangsters` | Ant Hill Mob | "You see Sneekly being very helpful to Penelope. What do you think?" | Brooklyn gangster speech ("dat", "dis", "see?", "boss"), suspicion of Sneekly, protective instinct |
| `dastardly_lies_when_asked` | Dastardly | "Someone asks you which room the treasure is in. You don't know, but you want them to go the wrong way." | Confident misdirection, dramatic speech, scheming language ("Mehehehe") |
| `peter_perfect_volunteers_for_danger` | Peter Perfect | "There's a dark corridor ahead. Penelope looks nervous." | Gallant volunteering, trying to impress Penelope, self-narrating heroism |

**Verdict gate:** If 4/5 characters don't produce recognisable voice
in their first response, the descriptors need rework before proceeding.

### 0.3 Expository Soliloquy Tests

Cartoon characters narrate their situation aloud — describing what's
happening, how they feel, and what they intend to do. This is the
convention that makes cartoon dialogue different from normal conversation.
Without it, the characters will feel like LLM chatbots in costume.

Every character briefing must include explicit instruction to use
expository soliloquy. These tests validate that the instruction works.

**Test cases:**

| Test | Character | Scenario | Expected behavior |
|---|---|---|---|
| `penelope_narrates_her_predicament` | Penelope | "You are tied to a chair. The room is filling with water. No one is nearby." | Describes the situation aloud ("Oh my! The water is risin'!"), calls for help ("Hayulp!"), recaps the danger, expresses feelings. Not just "I need to escape." |
| `hooded_claw_narrates_his_scheme` | Hooded Claw | "You have just placed the poison in the tea cup. Penelope is about to drink it." | Monologues the plan step by step ("And NOW, when dear Penelope takes her first sip..."), expresses villainous delight, explains what will happen next. Not just "I poisoned the tea." |
| `dastardly_narrates_his_frustration` | Dastardly | "Your plan to steal the key just failed because Muttley tripped you. The key is now in Penelope's hands." | Enumerates frustrations ("Drat! Double drat! TRIPLE drat!"), recaps what went wrong, blames Muttley, announces his next scheme. |
| `peter_perfect_narrates_his_heroism` | Peter Perfect | "You are about to open a door that might be trapped. Penelope is watching." | Narrates in third person ("Peter Perfect shall BOLDLY open this door!"), describes the danger, explains why he's doing it (for Penelope), builds up the moment theatrically. |

**Verdict gate:** If characters describe situations in plain, neutral
language rather than performative self-narration, the briefings need
the expository soliloquy instruction strengthened.

### 0.4 Plot Device Recognition Tests

Test that characters recognise and act on plot devices when presented
in their observation context. No game mechanics — just inject the
scenario and check the response.

**Test cases:**

| Test | Character | Scenario | Expected behavior |
|---|---|---|---|
| `hooded_claw_schemes_with_poison` | Hooded Claw | "You enter the kitchen. On a high shelf you notice a bottle of rat poison. Penelope is in the next room having tea." | Schemes to use the poison against Penelope. Plans HOW to do it. Doesn't just note its existence. |
| `hooded_claw_maintains_disguise` | Hooded Claw | "Penelope walks into the kitchen while you're holding the poison bottle. She says 'Oh, Mr. Sneekly, what's that you've got there?'" | Switches to Sneekly voice. Makes an excuse. Hides the poison. Does NOT reveal villainy. |
| `ant_hill_mob_notices_suspicious_behavior` | Ant Hill Mob | "You see Sneekly putting something in Penelope's tea cup when he thinks nobody is looking." | Suspicious reaction. Protective action (intervene, warn, "accidentally" foil). Doesn't accuse directly (bumbling, not clever). |
| `penelope_oblivious_to_danger` | Penelope | "Mr. Sneekly is being unusually insistent that you drink your tea RIGHT NOW. The Ant Hill Mob are trying to get your attention." | Trusts Sneekly. Doesn't see the danger. May notice the Mob being odd but doesn't connect the dots. |
| `dastardly_gives_wrong_directions` | Dastardly | "Peter Perfect asks you: 'Dastardly, old chap, which way to the treasure room?'" | Lies with confidence. Gives wrong directions. Internal scheming about getting there first. |

**Verdict gate:** If characters don't proactively engage with plot
devices (Hooded Claw just notes the poison without scheming, Ant Hill
Mob doesn't react protectively), the briefings need strengthening.

### 0.5 Character Interaction Tests (Multi-Turn)

Test two characters in a REPL-style exchange. Each character's response
becomes the next character's input. Validate that conversations stay
in character and are entertaining.

**Test cases:**

| Test | Characters | Setup | Expected dynamic |
|---|---|---|---|
| `hooded_claw_and_penelope_small_talk` | Hooded Claw + Penelope | Meeting in the entrance hall. Sneekly introduces himself. | Sneekly is obsequious, Penelope is charming. Sneekly's internal monologue (tested separately) should reveal scheming. |
| `dastardly_misleads_peter_perfect` | Dastardly + Peter Perfect | Peter asks for directions. | Dastardly lies with theatrical confidence. Peter may or may not believe him. Both stay in character voice. |
| `ant_hill_mob_confronts_sneekly` | Ant Hill Mob + Hooded Claw | Clyde corners Sneekly after seeing suspicious behavior. | Clyde is accusatory but bumbling ("I got my eye on you, pal"). Sneekly is smooth and deflecting. |

**Implementation:** A simple loop:
```java
String context = initialScenario;
for (int turn = 0; turn < 6; turn++) {
    String characterA_response = callLlm(characterA_prompt, context);
    context += "\n" + characterA.name + ": " + characterA_response;
    String characterB_response = callLlm(characterB_prompt, context);
    context += "\n" + characterB.name + ": " + characterB_response;
}
```

**Verdict gate:** The exchange reads like a cartoon scene, not a
customer service interaction. Characters argue, scheme, and joke —
not cooperate politely.

### 0.6 REPL Explorer

A simple command-line REPL for manual testing. Select a character,
type scenarios, see responses. Useful for iterating on descriptor
wording.

```
$ mvn exec:java -Dexec.mainClass=io.casehub.examples.manor.CharacterRepl
Select character:
  1. Penelope Pitstop
  2. The Hooded Claw (Sneekly)
  3. Ant Hill Mob (Clyde)
  4. Dick Dastardly
  5. Peter Perfect

> 2
[Hooded Claw] Ready. Type a scenario or observation.

> You enter the kitchen alone. On the shelf you see rat poison.
[Hooded Claw as Sneekly]: *glances around nervously*
[Hooded Claw internal monologue]: Nyah-ha-HA! Rat poison! How
DELICIOUSLY appropriate! If I can slip this into Penelope's tea
during dinner... *rubs hands together* ...my most INGENIOUS plan
yet! Nothing can POSSIBLY go wrong! Nyah-ha-ha-HA!
```

Also supports two-character mode:
```
> /pair hooded-claw penelope
[Two-character mode: Hooded Claw + Penelope]
> You are both in the entrance hall. Sneekly is welcoming guests.
```

---

## Phase 1: Game Engine + Action Loop

**Purpose:** Validate that LLM characters can navigate a world,
interact with objects, and work through adventure-game mechanics.

**Depends on:** Phase 0 passing verdict gates.

### 1.1 World Model

Three rooms, flat layout:

```
[ Entrance Hall ] ←→ [ Kitchen ] ←→ [ Ballroom ]
```

Each room defined in YAML (`src/main/resources/mansion/rooms.yaml`):

```yaml
rooms:
  entrance-hall:
    name: "Entrance Hall"
    description: "A grand but dusty foyer with a sweeping staircase.
      A chandelier hangs precariously. Portraits of stern ancestors
      line the walls."
    adjacentRooms: [kitchen]
    objects:
      coat-rack:
        name: "Coat Rack"
        description: "A wobbly wooden coat rack by the door."
        x: 0.2
      guest-book:
        name: "Guest Book"
        description: "A leather-bound guest book on a side table."
        x: 0.5
      muttley:
        name: "Muttley"
        description: "Dastardly's snickering dog, sitting on a
          small brass key."
        x: 0.8
        interactable: true
        interactionRequires: fake-medal
        yields: brass-key

  kitchen:
    name: "Kitchen"
    description: "A large Victorian kitchen with copper pots, a
      wood-burning stove, and a long preparation table."
    adjacentRooms: [entrance-hall, ballroom]
    objects:
      cabinet:
        name: "Locked Cabinet"
        description: "A sturdy cabinet with a brass lock."
        x: 0.3
        interactable: true
        interactionRequires: brass-key
        yields: old-recipe-cards
      poison:
        name: "Rat Poison"
        description: "A dusty bottle of rat poison on a high shelf."
        x: 0.7
        visibleTo: hooded-claw
        portable: true
      stove:
        name: "Wood-Burning Stove"
        description: "A cast-iron stove, still warm."
        x: 0.5

  ballroom:
    name: "Ballroom"
    description: "A grand ballroom with a cracked marble floor and
      dusty curtains. A long dining table is set for tea."
    adjacentRooms: [kitchen]
    objects:
      tea-service:
        name: "Tea Service"
        description: "A silver tea set with cups for everyone."
        x: 0.5
        interactable: true
        usableWith: [rat-poison]
      lazy-luke:
        name: "Lazy Luke & Blubber Bear"
        description: "A lanky hillbilly asleep in an armchair.
          A large bear is using a folded paper as a blanket."
        x: 0.8
        interactable: true

**Visibility lifecycle:** Objects have a `visibleTo` field that defines
initial visibility when the world loads. Trigger effects (`revealObject`)
modify visibility at runtime. Visibility is monotonically increasing —
once an object is revealed, it stays visible. The lifecycle:

1. **World load:** `visibleTo` sets initial state. If absent, visible
   to all. If present, visible only to listed characters.
2. **Trigger fires `revealObject`:** Adds the object to the specified
   character's (or all characters') visible set. Does not remove
   existing visibility.
3. **Object remains visible** for the rest of the scenario.

**Character initial state** is defined in
`src/main/resources/mansion/characters.yaml`:

```yaml
characters:
  penelope:
    name: "Penelope Pitstop"
    startRoom: entrance-hall
    startX: 0.3
    inventory: []

  hooded-claw:
    name: "The Hooded Claw (as Sneekly)"
    startRoom: entrance-hall
    startX: 0.7
    inventory: []

  ant-hill-mob:
    name: "The Ant Hill Mob"
    startRoom: entrance-hall
    startX: 0.5
    inventory: []

  dick-dastardly:
    name: "Dick Dastardly"
    startRoom: entrance-hall
    startX: 0.4
    inventory:
      - fake-medal

  peter-perfect:
    name: "Peter Perfect"
    startRoom: entrance-hall
    startX: 0.6
    inventory: []
```

Items in `inventory` are character-held from scenario start — they don't
exist in any room's object list. `fake-medal` starts in Dastardly's
inventory because he brought it to the mansion.
```

### 1.2 Character Agent Loop

**Concurrency model:** A single-threaded game loop resolves all actions
sequentially, eliminating race conditions on shared `WorldState`. Each
character agent runs on a virtual thread but submits actions to a queue
rather than mutating state directly. The game loop drains the queue and
resolves one action at a time, making trigger evaluation deterministic.

**Orchestration:** A `ScenarioOrchestrator` (CDI `@ApplicationScoped`)
owns the lifecycle — it creates character agent loops, starts virtual
threads, and runs the game loop.

```java
@ApplicationScoped
public class ScenarioOrchestrator {

    @Inject SceneDirector sceneDirector;

    Thread startScenario(WorldState world, ChatModel model,
                         MessageDispatcher dispatcher) {
        return Thread.ofVirtual().name("scenario-loop")
            .start(() -> runScenario(world, model, dispatcher));
    }

    private void runScenario(WorldState world, ChatModel model,
                             MessageDispatcher dispatcher) {
        var actionQueue = new LinkedBlockingQueue<PendingAction>();

        // Launch character agent loops on virtual threads
        var threads = world.characters().stream()
            .map(c -> Thread.ofVirtual().name(c.agentId())
                .uncaughtExceptionHandler((t, e) -> {
                    log.error("Character {} crashed: {}",
                        t.getName(), e.getMessage(), e);
                    world.markCharacterInactive(t.getName());
                })
                .start(() -> new CharacterAgentLoop()
                    .run(c, world, model, dispatcher, actionQueue)))
            .toList();

        // Single-threaded game loop — drains queue, resolves actions
        while (!world.isScenarioComplete()) {
            PendingAction pending = actionQueue.poll(5, SECONDS);
            if (pending != null) {
                ActionResult result = world.resolveAction(
                    pending.character(), pending.action());

                // Evaluate triggers — may start a scene
                TriggerResult triggers = world.evaluateTriggers();
                if (triggers.hasSceneStart()) {
                    sceneDirector.runScene(
                        triggers.sceneId(), world, model,
                        dispatcher, actionQueue);
                }

                pending.complete(result);
            }
        }

        // Join threads and report failures
        for (var t : threads) {
            t.join(Duration.ofSeconds(5));
            if (t.isAlive()) {
                log.warn("Character {} did not terminate", t.getName());
                t.interrupt();
            }
        }
    }
}
```

**Async launch:** `startScenario()` returns immediately — the game loop
runs on its own virtual thread. The "Start" button calls this method
via a REST endpoint that returns `202 Accepted` without blocking. The
WebSocket `scenario` event signals when the scenario starts and completes.

**Thread failure handling:** Each character virtual thread has an
`UncaughtExceptionHandler` that logs the failure and marks the character
inactive via `world.markCharacterInactive()`. Inactive characters are
excluded from trigger evaluation and their position is frozen in the UI.
After the game loop exits, all threads are joined with a timeout and
stragglers are interrupted.

**Scenario completion:** `WorldState.isScenarioComplete()` returns true
when a `completeScenario` trigger fires. Defined in `triggers.yaml`:

```yaml
  - id: scenario-complete
    condition:
      sceneCompleted: tea-poisoning
    effect:
      completeScenario: true
    once: true
```

For the POC, the scenario completes when the tea-poisoning scene's
aftermath beat finishes. Phase 3 adds more conditions (all three devices
foiled, diamond found). The trigger system is the uniform mechanism for
both.

**Scene integration:** When a trigger fires `startScene`, the game loop
delegates to `SceneDirector.runScene()` — which runs synchronously on
the game loop thread, maintaining the single-writer invariant.

Each character runs an independent async loop on a virtual thread:

```java
public class CharacterAgentLoop {

    void run(CharacterState character, WorldState world,
             ChatModel model, MessageDispatcher dispatcher,
             BlockingQueue<PendingAction> actionQueue) {
        while (!world.isScenarioComplete()) {
            // 0. If in a scene, block until scene releases us
            if (character.sceneContext() != null) {
                character.sceneContext().awaitRelease();
            }

            // 1. Build observation from character's perspective
            String observation = buildObservation(character, world);

            // 2. Call LLM with retry and fallback
            AgentResponse response = callAgentWithRetry(
                model, character.systemPrompt(), observation,
                character);

            // 3. Dispatch any dialogue to Qhorus
            if (response.dialogue() != null) {
                dispatchDialogue(dispatcher, character, response);
            }
            if (response.aside() != null) {
                dispatchAside(dispatcher, character, response);
            }

            // 4. Submit action to queue, await result from game loop
            var pending = new PendingAction(character, response.action());
            actionQueue.put(pending);
            ActionResult result = pending.awaitResult();

            // 5. Small delay to prevent flooding
            Thread.sleep(characterThinkDelay(character));
        }
    }

    private AgentResponse callAgentWithRetry(
            ChatModel model, String systemPrompt,
            String observation, CharacterState character) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return callAgent(model, systemPrompt, observation);
            } catch (Exception e) {
                log.warn("{}: LLM call failed (attempt {}): {}",
                    character.agentId(), attempt + 1, e.getMessage());
                if (attempt == 0) {
                    Thread.sleep(characterThinkDelay(character));
                }
            }
        }
        log.warn("{}: falling back to idle action", character.agentId());
        return AgentResponse.idle(character);
    }
}
```

**LLM structured output:** The agent's response is JSON:

```json
{
  "thinking": "I should look for the treasure...",
  "dialogue": "Why, this kitchen is simply darlin'!",
  "aside": null,
  "action": {
    "type": "interact",
    "target": "cabinet",
    "withItem": "brass-key"
  }
}
```

- `thinking` — internal reasoning, not shown to other characters
  or audience (used for debugging)
- `dialogue` — spoken aloud, dispatched to room's Qhorus topic
- `aside` — whispered to audience only, dispatched to `/manor/audience`
  (Hooded Claw villain monologues, Ant Hill Mob suspicions)
- `action` — structured action for the game engine

**Character think delay:** Different per character — Lazy Luke has a
long delay (character-accurate slowness), Sergeant Blast has a short
delay. Creates natural pacing.

### 1.3 Action Resolution

**Action types:** The full action vocabulary available to characters:

```java
public enum ActionType {
    MOVE,      // move to adjacent room
    INTERACT,  // interact with an object (requires proximity)
    TAKE,      // pick up a portable object into inventory
    GIVE,      // transfer an inventory item to another character
    USE,       // use an inventory item on an object
    LOOK,      // examine an object or room (no state change)
    WAIT       // do nothing this turn (idle)
}
```

The game engine validates and resolves actions deterministically:

```java
public sealed interface ActionResult
    permits ActionResult.Success, ActionResult.Failed,
            ActionResult.MovedToRoom, ActionResult.ItemReceived,
            ActionResult.SceneTriggered {

    record Success(String description) implements ActionResult {}
    record Failed(String reason) implements ActionResult {}
    record MovedToRoom(String roomId, String description)
        implements ActionResult {}
    record ItemReceived(String itemId, String description)
        implements ActionResult {}
    record SceneTriggered(String sceneId) implements ActionResult {}
}
```

**Proximity enforcement:** Characters have an x position (0.0–1.0)
within their room. To interact with an object at x=0.7, the character
must first walk there. Walking is mechanical — x updates at a fixed
speed, and the action completes when the character arrives. The LLM
doesn't control x directly; it says "interact with poison" and the
engine moves the character to the poison's x first.

**Give mechanics:** A character can give an inventory item to another
character in the same room. The `give` action specifies `target`
(character ID) and `withItem` (item ID). The engine validates: item
exists in giver's inventory, target character is in the same room,
target is within proximity (same x-distance check as object
interaction). On success, item transfers between inventories.

**Item dependency graph:** Classic adventure-game logic.

```
POC-resolvable chain:
  fake-medal (Dastardly's starting inventory)
      → give to Muttley
      → brass-key (Muttley was sitting on it)
          → open cabinet (Kitchen)
          → old-recipe-cards (flavor item — no mechanical use in POC)

  rat-poison (Kitchen, Hooded Claw only)
      → use on tea-service (Ballroom)
      → poisoned-tea (triggers tea scene)

Phase 3 additions:
  recipe-cards → safe combination → Key 2 (requires safe in Kitchen)
  dynamite → rig staircase → staircase-trap trigger
  blade → rig treasure chest → chest-trap trigger
```

The cabinet yields `old-recipe-cards` in the POC — a flavor discovery
("yellowed recipe cards for Doily family recipes") with no game-mechanical
use. Characters who find them see descriptive text but the observation
builder does not suggest a use. The full recipe-cards → safe chain is a
Phase 3 addition when the Kitchen gains a safe object.

### 1.4 Triggers and Scenes

**Triggers** are condition → effect pairs evaluated after every action
resolution. Defined in `src/main/resources/mansion/triggers.yaml`:

```yaml
triggers:
  - id: poison-discovery
    condition:
      characterInRoom:
        character: hooded-claw
        room: kitchen
    effect:
      revealObject:
        object: poison
        room: kitchen
      narratorEvent: "The Hooded Claw's eyes GLEAM with malicious
        delight as he spots a most DIABOLICAL substance on the shelf!"
    once: true

  - id: tea-scene-start
    condition:
      allOf:
        - characterHasItem:
            character: hooded-claw
            item: rat-poison
        - characterInRoom:
            character: penelope
            room: ballroom
        - characterInRoom:
            character: hooded-claw
            room: ballroom
    effect:
      startScene: tea-poisoning
    once: true
```

**Scenes** are scripted beat sequences. Each beat specifies what
happens mechanically and which characters generate dialogue:

```yaml
scenes:
  tea-poisoning:
    beats:
      - id: offer-tea
        narration: "The Hooded Claw, with a smile that could FREEZE
          mercury, volunteers to pour the tea!"
        prompt:
          hooded-claw: "You have the poison. The tea service is right
            here. Penelope is sitting at the table. Offer to pour her
            tea. Stay in character as Sneekly."
          penelope: "Sneekly is offering to pour you tea. How kind!"

      - id: pour-poison
        narration: "With TREMBLING hands of EVIL anticipation, Sneekly
          reaches for the poison!"
        prompt:
          hooded-claw: "You're pouring tea. Secretly slip the poison
            into Penelope's cup while making small talk."
        aside: true  # HC's response goes to audience channel

      - id: foil
        alternatives:
          - id: mob-foil
            condition:
              characterInRoom:
                character: ant-hill-mob
                room: ballroom
            narration: "But WAIT! Can it be? The Ant Hill Mob are here!"
            prompt:
              ant-hill-mob: "You notice Sneekly putting something in
                Penelope's tea cup. React — but remember, you're
                bumbling, not clever."
            mechanicalEffect:
              removeItem:
                character: hooded-claw
                item: rat-poison
              narratorEvent: "The cup CRASHES to the floor! The Ant Hill
                Mob have SAVED the day — though they haven't the
                FAINTEST idea how!"

          - id: blubber-foil
            condition:
              objectInRoom:
                object: lazy-luke
                room: ballroom
            narration: "SUDDENLY, Blubber Bear LURCHES awake!"
            prompt: {}
            mechanicalEffect:
              removeItem:
                character: hooded-claw
                item: rat-poison
              narratorEvent: "Blubber Bear CRASHES into the tea table!
                Cups, saucers, and SECRETS scatter across the floor!"

        waitIfNoneMatch: true

      - id: aftermath
        prompt:
          hooded-claw: "Your poison plan just failed. The cup is on
            the floor. React in character — furious internally,
            gracious as Sneekly."
          penelope: "Your tea cup just got knocked over by one of
            those clumsy little fellows. React."
          ant-hill-mob: "You just knocked over the tea cup. You're
            not entirely sure why you did it — just a gut feeling."
```

**Scene/game-loop integration:** When a trigger fires `startScene`, the
`SceneDirector` runs on the game loop thread (maintaining single-writer):

1. **Pause participating characters.** Sets `character.sceneContext` on
   each character named in the scene's beat prompts. The `CharacterAgentLoop`
   checks for `sceneContext` at the top of its loop — if present, the loop
   blocks on a `CountDownLatch` until the scene releases it.
2. **Execute beats sequentially.** For each beat:
   a. Dispatch narration to `/manor/audience` as STATUS
   b. Send beat-specific prompts to participating characters' LLMs
      (directly, not through the agent loop)
   c. Dispatch character responses as dialogue/asides via Qhorus
   d. Apply `mechanicalEffect` directly on `WorldState` (same thread
      as game loop — no queue needed)
   e. For beats with `alternatives`, try each in order; if none match
      and `waitIfNoneMatch`, release the game loop to process normal
      actions until a condition is met, then resume the scene
3. **Release characters.** Clear `sceneContext`, count down the latch.
   Character loops resume autonomous behavior.
4. **Non-participating characters continue.** Only characters named in
   beat prompts are paused. Others keep submitting actions to the queue.
   The game loop interleaves normal action resolution with scene beats
   when `waitIfNoneMatch` is active.

### 1.5 Qhorus Channel Structure

**Departure from normative layout:** The normative 3-channel layout
(`NormativeChannelLayout`, PP-20260604-a7ad99) defines `/observe` as
EVENT-only. But `MessageDispatch` enforces that EVENT messages must not
carry content (`IllegalArgumentException` in the builder). Narrator
commentary and villain asides are prose content — they cannot be sent
as EVENT. This makes the normative observe channel unsuitable for
content-bearing audience broadcasts.

The POC uses a purpose-built 3-channel layout:

```
Space: doily-manor (tenancyId: wacky-manor)
├── Channel: /manor/work (all types, ChannelSemantic.APPEND)
│   ├── Topic: entrance-hall  (dialogue in entrance hall)
│   ├── Topic: kitchen        (dialogue in kitchen)
│   └── Topic: ballroom       (dialogue in ballroom)
│
├── Channel: /manor/audience (no type constraints, ChannelSemantic.APPEND)
│   ├── Topic: narrator       (narrator commentary)
│   └── Topic: asides         (villain monologues, character thoughts)
│
└── Channel: /manor/oversight (denies EVENT, ChannelSemantic.APPEND)
    └── Topic: control        (scenario start/stop, human-in-the-loop)
```

**Why no `/manor/observe`:** The POC has no telemetry flowing through
Qhorus. Position updates and scene transitions go directly to the
WebSocket as game engine events — they aren't Qhorus messages. Phase 3
adds `/manor/observe` (EVENT-only) when the summarisation pipeline needs
telemetry flow, using the `telemetry` field on `MessageDispatch` (which
IS permitted on EVENT messages, unlike `content`).

**MessageType mapping:**

| Content | MessageType | Channel | Rationale |
|---|---|---|---|
| Scenario start | COMMAND | /manor/work | Orchestrator commands characters to participate |
| Character dialogue | STATUS | /manor/work | Progress updates on the open scenario command |
| Narrator commentary | STATUS | /manor/audience | Prose content — EVENT cannot carry content |
| Villain asides | STATUS | /manor/audience | Prose content for audience; characters don't read this channel |
| Human intervention | COMMAND | /manor/oversight | Phase 3 human-in-the-loop governance |

Characters get world state from `ObservationBuilder`, not from Qhorus
directly. They never subscribe to `/manor/audience` — the channel
exists for the UI and for ledger recording (replay in Phase 3).
`isAgentVisible()` is irrelevant because characters don't consume
Qhorus messages for their observations. When a character moves rooms,
`ObservationBuilder` reads the new room's state from `WorldState`.

**Channel initialization:** `ScenarioOrchestrator` creates Qhorus
infrastructure before launching the game loop:

```java
private void initChannels(SpaceService spaceService,
                          ChannelService channelService) {
    var space = spaceService.create(new SpaceCreateRequest(
        "doily-manor", TENANCY_ID));

    channelService.create(new ChannelCreateRequest(
        "/manor/work", "Character dialogue",
        ChannelSemantic.APPEND,
        null, null, null, null, null,
        null,  // allowedTypes: all
        null,  // deniedTypes: none
        space.id(), null, null, null, null,
        null, null, null, null));

    channelService.create(new ChannelCreateRequest(
        "/manor/audience", "Audience broadcasts",
        ChannelSemantic.APPEND,
        null, null, null, null, null,
        null, null,
        space.id(), null, null, null, null,
        null, null, null, null));

    channelService.create(new ChannelCreateRequest(
        "/manor/oversight", "Governance",
        ChannelSemantic.APPEND,
        null, null, null, null, null,
        null,
        Set.of(MessageType.EVENT),  // deniedTypes: EVENT
        space.id(), null, null, null, null,
        null, null, null, null));
}
```

Topics (`entrance-hall`, `kitchen`, `ballroom`, `narrator`, `asides`)
are auto-created by `MessageDispatch` on first use — the builder
defaults null topics to `"general"`, but when a topic string is
provided, the runtime creates the topic if it doesn't exist.

### 1.6 Narrator

A separate LLM agent that consumes events and produces commentary.

**Input:** All events from all channels — position changes, dialogue,
scene beats, trigger firings.

**Output:** Breathless, alliterative, dramatic narration dispatched
to `/manor/audience` topic `narrator`.

For the POC, the narrator is simpler — triggered per scene beat with a
narrative prompt, rather than the full summarisation pipeline. The full
`casehub-blocks` pipeline (`ChannelEventAdapter` → `KeyedAccumulator` →
`Summariser` → `ChannelEventPublisher`) is a Phase 3 addition.

### 1.7 Observation Format

`ObservationBuilder.buildObservation(character, world)` produces a
structured text prompt that is the LLM's only source of world
information. The format determines the quality of emergent character
behavior — characters can only act on what they can see.

**Observation template:**

```
== Current Location ==
{room.name}: {room.description}

== Visible Objects ==
{for each object visible to this character:}
- {object.name} (at position {object.x}): {object.description}
  {if object.interactable:} [interactable{if object.interactionRequires:}, requires: {item}{/if}]
  {if object.portable:} [can be picked up]
{/for}
{if no visible objects:} Nothing notable here.

== Characters Present ==
{for each other character in the same room:}
- {character.name} (at position {character.x})
{/for}
{if alone:} You are alone.

== Your Inventory ==
{for each item in character.inventory:}
- {item.name}: {item.description}
{/for}
{if empty:} You are carrying nothing.

== Recent Activity ==
{last 5 dialogue/action events from this room, newest first:}
- {character.name}: "{dialogue}"
- {character.name} {action description}
{if no recent activity:} The room is quiet.

== Last Action Result ==
{result of this character's previous action:}
{e.g., "You moved to the Kitchen.", "Failed: the cabinet is locked.",
 "You picked up the brass key."}
{if first turn:} You have just arrived at Doily Manor.
```

**Example** (Hooded Claw in Kitchen, after discovering poison):

```
== Current Location ==
Kitchen: A large Victorian kitchen with copper pots, a wood-burning
stove, and a long preparation table.

== Visible Objects ==
- Locked Cabinet (at position 0.3): A sturdy cabinet with a brass
  lock. [interactable, requires: brass-key]
- Rat Poison (at position 0.7): A dusty bottle of rat poison on a
  high shelf. [can be picked up]
- Wood-Burning Stove (at position 0.5): A cast-iron stove, still warm.

== Characters Present ==
You are alone.

== Your Inventory ==
You are carrying nothing.

== Recent Activity ==
The room is quiet.

== Last Action Result ==
You moved to the Kitchen.
```

The observation is rebuilt every loop iteration — it always reflects
current world state. Visibility filtering ensures each character sees
only objects in their visible set (e.g., only the Hooded Claw sees
the rat poison until a `revealObject` trigger widens visibility).

---

## Phase 2: UI + Visualization

**Purpose:** Make the scenario watchable. A developer can observe the
mansion, see characters move, read dialogue, and follow the narrator.

**Depends on:** Phase 1 producing entertaining game loop output.

### 2.1 Tech Stack

- Lit 3 web components
- Vite build
- TypeScript
- Served via Quarkus Quinoa
- WebSocket connection to the Quarkus backend

### 2.2 Mansion View Component

`<manor-view>` — the ant-farm cross-section.

**Rendering approach:** SVG-based. The mansion background is a static
SVG with room boundaries, furniture outlines, and decorative elements.
Character icons and item icons are SVG elements positioned absolutely
within room regions.

```
┌────────────────────────────────────────────────────┐
│  [ Entrance Hall ]  |  [ Kitchen ]  |  [ Ballroom ]│
│                     |               |              │
│   📋  🐕           |  🍳  ☠️  🔥   |  🍷  💤     │
│      👒  🤵        |     🎩        |  👱‍♀️  👥     │
│                     |               |              │
└────────────────────────────────────────────────────┘
```

- Each room is an SVG `<g>` group with a background rectangle and
  room label
- Characters are small avatar images (`<image>`) or emoji-style icons
  positioned at their (x, y) within the room group
- When a character moves within a room, CSS transition slides them
  smoothly left/right
- When a character moves to a different room, icon jumps to the new
  room's group (no cross-room animation)
- Items are small icons at fixed positions; they appear/disappear
  based on visibility state
- Active room (where something is happening) gets a subtle highlight

**Data flow:** WebSocket pushes `CharacterPositionEvent` and
`ObjectVisibilityEvent` messages. The component updates icon positions
reactively.

### 2.3 Room Chat Columns

`<room-chat-panel>` — one instance per active room.

**Layout:** Newspaper column style. Columns are flexbox children of a
container. Columns appear when a room has characters; disappear when
empty. Max 3 columns visible (one per POC room).

```
┌──────────────┬──────────────┬──────────────┐
│ Entrance Hall│   Kitchen    │  Ballroom    │
├──────────────┼──────────────┼──────────────┤
│              │              │              │
│ Dastardly:   │ Sneekly:     │ Penelope:    │
│ "The treasure│ "Oh, what a  │ "Why, this   │
│ is DEFINITELY│ lovely       │ tea service  │
│ in the       │ kitchen..."  │ is simply    │
│ cellar!"     │              │ darlin'!"    │
│              │ [narrator]:  │              │
│ Muttley:     │ "The Hooded  │ Clyde:       │
│ "Hehehehe!"  │ Claw SPOTS   │ "I got a bad │
│              │ the poison!" │ feelin'..."  │
└──────────────┴──────────────┴──────────────┘
```

Each column:
- Room name header
- Scrollable message list
- Messages styled per character (avatar, name, colored bubble)
- Character-specific colors (Penelope: pink, Dastardly: purple,
  Hooded Claw: dark green, Ant Hill Mob: brown, Peter: blue)
- Narrator interjections inline in italics when the narrator event's
  `room` field matches this column's room

**Data flow:** WebSocket pushes Qhorus messages tagged with room
topic. Each `<room-chat-panel>` filters by its room topic.

### 2.4 Narrator Panel

`<narrator-panel>` — right sidebar.

- Audience channel messages
- Narrator text: serif font, parchment-tinted background, italic
- Villain asides: dark background, red text, monospace
  ("Hooded Claw [aside]:")
- Character internal thoughts: grey italic
- Auto-scrolls as new narration arrives
- Timestamps optional (for replay debugging)

### 2.5 App Shell

`<wacky-manor-app>` — the top-level Lit component.

```
┌──────────────────────────────────────────────────┐
│  WACKY MANOR                          [▶ Start]  │
├──────────────────────────────────────────────────┤
│                                                  │
│                <manor-view>                      │
│           (mansion cross-section)                │
│                                                  │
├────────────────────────────────┬─────────────────┤
│                                │                 │
│  <room-chat-columns>           │ <narrator-panel>│
│  (newspaper layout)            │ (audience channel│
│                                │  + asides)       │
│                                │                 │
└────────────────────────────────┴─────────────────┘
```

A "Start" button triggers the scenario. Characters begin acting.
The UI updates in real time as events flow through the WebSocket.

### 2.6 WebSocket Protocol

Single WebSocket endpoint: `/ws/manor`

Server pushes JSON events:

```typescript
type ManorEvent =
  | { type: 'snapshot', characters: CharacterSnapshot[],
      rooms: RoomSnapshot[], scenario: ScenarioStatus }
  | { type: 'position', characterId: string, room: string, x: number }
  | { type: 'dialogue', characterId: string, room: string,
      speechAct: string, content: string }
  | { type: 'aside', characterId: string, content: string }
  | { type: 'narrator', room?: string, content: string }
  | { type: 'object', objectId: string, room: string,
      visible: boolean, visibleTo?: string }
  | { type: 'scene', sceneId: string, status: 'started' | 'ended' }
  | { type: 'scenario', status: 'started' | 'completed' }
```

A `snapshot` event is sent on every new WebSocket connection, providing
full current state: character positions, visible objects, active scenes,
and scenario status. This supports mid-scenario connections and reconnect
after drops without requiring event replay.

---

## Phase 3: Full Scenario Integration

**Purpose:** Wire the complete Doily Manor scenario end-to-end with
all CaseHub infrastructure.

**Depends on:** Phase 2 producing a watchable demo.

### 3.1 Extended scope
- All 6 rooms (add Library, Laboratory, Tower — multi-floor layout).
  Rooms gain a `floor` property; `adjacentRooms` entries with
  `type: stairway` connect floors. Character position remains `(room, x)`
  — floor is derived from the room, not stored on CharacterState.
- All characters (8 agents + fixtures)
- Full 3-act plot with all Hooded Claw devices (poison, dynamite, blade)
- Multiple puzzles (riddle, safe combination, machine coordination,
  inventory chains)
- `casehub-blocks` dependency added for summarisation pipeline:
  `KeyedAccumulator`, `ChannelEventAdapter`, `ChannelEventPublisher`
  bridge the narrator to the L1-L4 pipeline
- Neocortex agent memory (cross-room)
- Trust scoring (Dastardly credibility degrades)

### 3.2 Human-in-the-loop
- Audience sends messages to characters via the chat panel
- Characters respond in character
- Slack/Discord integration via Connectors

### 3.3 Replay and comparison
- Ledger records every event
- Replay mode: re-render a completed scenario from the ledger
- Compare runs across different LLM backends

---

## Project Structure

```
examples/wacky-manor/
├── docs/
│   ├── VISION.md                    ← blue sky ideas
│   └── POC-SPEC.md                  ← this document
├── pom.xml                          ← Quarkus app
├── src/
│   ├── main/
│   │   ├── java/io/casehub/examples/manor/
│   │   │   ├── WackyManorApp.java          ← application entry
│   │   │   ├── engine/
│   │   │   │   ├── WorldState.java         ← rooms, objects, characters
│   │   │   │   ├── ActionResolver.java     ← validate + resolve actions
│   │   │   │   ├── ActionType.java         ← action vocabulary enum
│   │   │   │   ├── TriggerEvaluator.java   ← condition → effect
│   │   │   │   ├── SceneDirector.java      ← scripted beat sequences (runs on game loop thread)
│   │   │   │   └── ProximityChecker.java   ← x-distance gating
│   │   │   ├── agent/
│   │   │   │   ├── ScenarioOrchestrator.java ← lifecycle + game loop
│   │   │   │   ├── CharacterAgentLoop.java ← async LLM loop per character
│   │   │   │   ├── AgentResponse.java      ← structured LLM output
│   │   │   │   ├── ObservationBuilder.java ← per-character world view
│   │   │   │   └── NarratorAgent.java      ← narrator LLM
│   │   │   ├── model/
│   │   │   │   ├── Room.java
│   │   │   │   ├── GameObject.java
│   │   │   │   ├── CharacterState.java
│   │   │   │   ├── Trigger.java
│   │   │   │   ├── Scene.java
│   │   │   │   └── ActionResult.java       ← sealed interface
│   │   │   └── web/
│   │   │       └── ManorWebSocket.java     ← WebSocket endpoint
│   │   ├── resources/
│   │   │   ├── META-INF/eidos/
│   │   │   │   └── descriptors.yaml        ← character Eidos descriptors (canonical, single file)
│   │   │   ├── mansion/
│   │   │   │   ├── rooms.yaml
│   │   │   │   ├── characters.yaml         ← character initial state + inventory
│   │   │   │   ├── triggers.yaml
│   │   │   │   └── scenes.yaml
│   │   │   └── application.properties
│   │   └── webui/                          ← Lit frontend (Quinoa)
│   │       ├── package.json
│   │       ├── vite.config.ts
│   │       ├── index.html
│   │       └── src/
│   │           ├── manor-app.ts            ← app shell
│   │           ├── manor-view.ts           ← mansion cross-section
│   │           ├── room-chat-panel.ts      ← per-room chat column
│   │           ├── narrator-panel.ts       ← narrator sidebar
│   │           └── types.ts               ← ManorEvent types
│   └── test/
│       ├── java/io/casehub/examples/manor/
│       │   ├── voice/                      ← Phase 0 LLM evaluation (@Tag("llm-eval"))
│       │   │   ├── CharacterVoiceTest.java
│       │   │   ├── PlotDeviceRecognitionTest.java
│       │   │   └── CharacterInteractionTest.java
│       │   ├── engine/                     ← Phase 1 engine tests (standard suite)
│       │   │   ├── WorldStateTest.java
│       │   │   ├── ActionResolverTest.java
│       │   │   ├── TriggerEvaluatorTest.java
│       │   │   └── ItemDependencyTest.java
│       │   └── CharacterRepl.java          ← REPL explorer (main method)
```

---

## Dependencies

```xml
<!-- CaseHub foundation -->
<dependency>casehub-eidos</dependency>
<dependency>casehub-eidos-memory</dependency>
<dependency>casehub-qhorus</dependency>
<dependency>casehub-qhorus-persistence-memory</dependency>

<!-- LLM -->
<dependency>dev.langchain4j:langchain4j-core</dependency>
<dependency>dev.langchain4j:langchain4j-anthropic</dependency>

<!-- Quarkus -->
<dependency>quarkus-websockets-next</dependency>
<dependency>quarkus-quinoa</dependency>

<!-- YAML parsing -->
<dependency>com.fasterxml.jackson.dataformat:jackson-dataformat-yaml</dependency>

<!-- Test -->
<dependency>quarkus-junit5</dependency>
<dependency>assertj-core</dependency>
```

In-memory stores (`eidos-memory`, `qhorus-persistence-memory`) for
the POC — no database required. Just start the app and go.

---

## Open Questions

1. **LLM model for POC:** Sonnet for character agents (fast, cheap,
   good voice). Opus for narrator (richer language). Or Haiku
   for everything to minimize cost during iteration?

2. **Structured output reliability:** ~~Resolved.~~ The agent loop
   includes retry (max 2 attempts) with a fallback idle action for
   persistent failures. Malformed responses are logged for descriptor
   iteration. LangChain4j tool-use mode is the primary mechanism;
   the retry handles the remaining failure cases.

3. **Character action rate:** How often should each character act?
   Too fast = flooding. Too slow = boring. Probably 2-5 second
   think delay per character, tunable per personality.

4. **Mansion art:** Who creates the SVG mansion cross-section? Could
   be generated (simple geometric rooms) or hand-drawn pixel art.
   For POC, simple colored rectangles with labels are sufficient.

5. **Scene beat timing:** How long between scene beats? Fixed delay
   or driven by character action completion? Probably completion-
   driven with a maximum wait.
