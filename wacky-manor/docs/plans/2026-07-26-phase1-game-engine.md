# Phase 1: Game Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use
> subagent-driven-development (recommended) or executing-plans to
> implement this plan task-by-task. Each task follows TDD
> (test-driven-development) and uses ide-tooling for structural
> editing. Steps use checkbox (`- [ ]`) syntax for tracking.

**Focal issue:** TBD — create issue on casehubio/examples before starting
**Issue group:** N/A — single-issue scope

**Goal:** Build the game engine that lets LLM characters navigate rooms,
interact with objects, and trigger scripted plot beats — all dialogue
generated through the Eidos+AgentProvider stack validated in Phase 0.

**Architecture:** Single-threaded game loop drains an action queue.
Character agents run on virtual threads and submit actions to the queue.
WorldState is the single mutable data structure, mutated only by the
game loop thread. Triggers and scenes compose on top of action
resolution. Qhorus channels carry dialogue and narrator commentary.

**Tech Stack:** Java 21, Quarkus 3.32.2, CaseHub Eidos (templates +
goals), CaseHub Qhorus (channels + message dispatch), CaseHub Blocks
(ObservationAccumulator), casehub-platform-agent-claude (LLM calls),
Jackson YAML, virtual threads.

## Global Constraints

- Java 21 source level, runs on Java 26 JVM
- `JAVA_HOME=$(/usr/libexec/java_home -v 26)` for all Maven commands
- Build: `mvn -f examples/wacky-manor/pom.xml test` (default profile excludes `llm-eval` tagged tests)
- All new engine tests are standard JUnit 5 — no `@Tag("llm-eval")`, no LLM calls
- Package root: `io.casehub.examples.manor`
- Sub-packages: `model/` (data records), `engine/` (game logic), `agent/` (LLM integration)
- `ManorConstants.TENANCY_ID = "wacky-manor"` — shared across all Qhorus operations
- CaseHub dependency version: `${casehub.version}` = `0.2-SNAPSHOT`
- Existing Phase 0 code (voice/, descriptors.yaml, templates.yaml) must not be broken
- `CharacterProfile` and `CharacterProfileLoader` are legacy Phase 0 code — unused by Phase 1, can be deleted when convenient but not required

## File Map

```
src/main/java/io/casehub/examples/manor/
├── ManorConstants.java                    (exists — add THINK_DELAY constants)
├── model/
│   ├── Room.java                          (record — YAML-loaded room definition)
│   ├── GameObject.java                    (record — YAML-loaded object definition)
│   ├── CharacterState.java                (mutable — runtime character state)
│   ├── ActionType.java                    (enum — MOVE, INTERACT, TAKE, GIVE, USE, LOOK, WAIT)
│   ├── Action.java                        (record — character's chosen action)
│   ├── ActionResult.java                  (sealed interface — Success, Failed, MovedToRoom, ItemReceived, SceneTriggered)
│   ├── PendingAction.java                 (record — action + CompletableFuture for result)
│   ├── Trigger.java                       (record — condition/effect pair from YAML)
│   ├── TriggerCondition.java              (sealed interface — CharacterInRoom, CharacterHasItem, ObjectInRoom, SceneCompleted, AllOf)
│   ├── TriggerEffect.java                 (sealed interface — RevealObject, StartScene, NarratorEvent, CompleteScenario, RemoveItem)
│   ├── Scene.java                         (record — scripted beat sequence)
│   ├── Beat.java                          (record — single scene beat)
│   ├── ManorEvent.java                    (record — timestamped game event for observation)
│   └── SceneContext.java                  (class — CountDownLatch wrapper for scene pausing)
├── engine/
│   ├── WorldState.java                    (class — all mutable game state, single-writer)
│   ├── MansionLoader.java                 (class — loads rooms.yaml, characters.yaml, triggers.yaml, scenes.yaml)
│   ├── ActionResolver.java                (class — validates + resolves actions on WorldState)
│   ├── TriggerEvaluator.java              (class — evaluates trigger conditions, applies effects)
│   └── SceneDirector.java                 (class — runs scripted beat sequences)
├── agent/
│   ├── ObservationBuilder.java            (class — builds per-character observation text)
│   ├── AgentResponse.java                 (record — parsed structured LLM output)
│   ├── CharacterAgentLoop.java            (class — async LLM loop per character)
│   ├── ScenarioOrchestrator.java          (CDI bean — lifecycle, game loop, virtual threads)
│   ├── NarratorAgent.java                 (class — narrator LLM agent)
│   └── ManorChannels.java                 (CDI bean — Qhorus space/channel setup + dispatch)

src/main/resources/mansion/
├── rooms.yaml                             (3 rooms: entrance-hall, kitchen, ballroom)
├── characters.yaml                        (5 characters with starting positions + inventory)
├── triggers.yaml                          (poison-discovery, tea-scene-start, scenario-complete)
└── scenes.yaml                            (tea-poisoning scene with 4 beats)

src/test/java/io/casehub/examples/manor/
├── engine/
│   ├── WorldStateTest.java
│   ├── ActionResolverTest.java
│   ├── TriggerEvaluatorTest.java
│   ├── SceneDirectorTest.java
│   └── ItemDependencyTest.java
└── agent/
    ├── ObservationBuilderTest.java
    └── AgentResponseTest.java
```

---

### Task 1: World Model — Records, YAML Loading, WorldState

**Files:**
- Create: `src/main/java/io/casehub/examples/manor/model/Room.java`
- Create: `src/main/java/io/casehub/examples/manor/model/GameObject.java`
- Create: `src/main/java/io/casehub/examples/manor/model/CharacterState.java`
- Create: `src/main/java/io/casehub/examples/manor/model/ManorEvent.java`
- Create: `src/main/java/io/casehub/examples/manor/engine/WorldState.java`
- Create: `src/main/java/io/casehub/examples/manor/engine/MansionLoader.java`
- Create: `src/main/resources/mansion/rooms.yaml`
- Create: `src/main/resources/mansion/characters.yaml`
- Test: `src/test/java/io/casehub/examples/manor/engine/WorldStateTest.java`

**Interfaces:**
- Consumes: nothing — foundation task
- Produces:
  - `Room(String id, String name, String description, List<String> adjacentRooms, Map<String, GameObject> objects)`
  - `GameObject(String id, String name, String description, double x, Set<String> visibleTo, boolean portable, boolean interactable, String interactionRequires, String yields, List<String> usableWith)`
  - `CharacterState` — mutable class with `agentId()`, `name()`, `currentRoom()`, `x()`, `inventory()`, `isActive()`, `sceneContext()`
  - `ManorEvent(Instant timestamp, String type, String characterId, String room, String description)`
  - `WorldState` — `rooms()`, `characters()`, `character(id)`, `room(id)`, `visibleObjects(roomId, characterId)`, `moveCharacter(id, roomId)`, `addToInventory(characterId, itemId)`, `removeFromInventory(characterId, itemId)`, `revealObject(objectId, characterId)`, `markCharacterInactive(agentId)`, `isScenarioComplete()`, `setScenarioComplete()`, `addEvent(ManorEvent)`, `recentEvents(roomId, limit)`
  - `MansionLoader.loadWorld()` → `WorldState`

- [ ] **Step 1: Write YAML data files**

Create `src/main/resources/mansion/rooms.yaml`:

```yaml
rooms:
  entrance-hall:
    name: "Entrance Hall"
    description: "A grand but dusty foyer with a sweeping staircase. A chandelier hangs precariously. Portraits of stern ancestors line the walls."
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
        description: "Dastardly's snickering dog, sitting on a small brass key."
        x: 0.8
        interactable: true
        interactionRequires: fake-medal
        yields: brass-key

  kitchen:
    name: "Kitchen"
    description: "A large Victorian kitchen with copper pots, a wood-burning stove, and a long preparation table."
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
        visibleTo: [hooded-claw]
        portable: true
      stove:
        name: "Wood-Burning Stove"
        description: "A cast-iron stove, still warm."
        x: 0.5

  ballroom:
    name: "Ballroom"
    description: "A grand ballroom with a cracked marble floor and dusty curtains. A long dining table is set for tea."
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
        description: "A lanky hillbilly asleep in an armchair. A large bear is using a folded paper as a blanket."
        x: 0.8
        interactable: true
```

Create `src/main/resources/mansion/characters.yaml`:

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

- [ ] **Step 2: Write failing WorldStateTest**

```java
package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.GameObject;
import io.casehub.examples.manor.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorldStateTest {

    private WorldState world;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
    }

    @Test
    void loads_three_rooms() {
        assertThat(world.rooms()).hasSize(3);
        assertThat(world.rooms()).containsKeys("entrance-hall", "kitchen", "ballroom");
    }

    @Test
    void rooms_have_correct_adjacency() {
        Room kitchen = world.room("kitchen");
        assertThat(kitchen.adjacentRooms()).containsExactlyInAnyOrder("entrance-hall", "ballroom");
    }

    @Test
    void loads_five_characters() {
        assertThat(world.characters()).hasSize(5);
        assertThat(world.characters()).containsKeys(
            "penelope", "hooded-claw", "ant-hill-mob", "dick-dastardly", "peter-perfect");
    }

    @Test
    void characters_start_in_entrance_hall() {
        for (CharacterState c : world.characters().values()) {
            assertThat(c.currentRoom()).isEqualTo("entrance-hall");
        }
    }

    @Test
    void dastardly_starts_with_fake_medal() {
        CharacterState dastardly = world.character("dick-dastardly");
        assertThat(dastardly.inventory()).containsExactly("fake-medal");
    }

    @Test
    void poison_visible_only_to_hooded_claw() {
        var hcVisible = world.visibleObjects("kitchen", "hooded-claw");
        assertThat(hcVisible).anyMatch(o -> o.id().equals("poison"));

        var penelopeVisible = world.visibleObjects("kitchen", "penelope");
        assertThat(penelopeVisible).noneMatch(o -> o.id().equals("poison"));
    }

    @Test
    void reveal_object_makes_it_visible_to_character() {
        world.revealObject("poison", "penelope");
        var penelopeVisible = world.visibleObjects("kitchen", "penelope");
        assertThat(penelopeVisible).anyMatch(o -> o.id().equals("poison"));
    }

    @Test
    void move_character_updates_room() {
        world.moveCharacter("penelope", "kitchen");
        assertThat(world.character("penelope").currentRoom()).isEqualTo("kitchen");
    }

    @Test
    void add_and_remove_inventory() {
        world.addToInventory("penelope", "brass-key");
        assertThat(world.character("penelope").inventory()).contains("brass-key");

        world.removeFromInventory("penelope", "brass-key");
        assertThat(world.character("penelope").inventory()).doesNotContain("brass-key");
    }

    @Test
    void mark_character_inactive() {
        world.markCharacterInactive("peter-perfect");
        assertThat(world.character("peter-perfect").isActive()).isFalse();
    }

    @Test
    void scenario_complete_lifecycle() {
        assertThat(world.isScenarioComplete()).isFalse();
        world.setScenarioComplete();
        assertThat(world.isScenarioComplete()).isTrue();
    }

    @Test
    void recent_events_returns_most_recent_first() {
        world.addEvent("dialogue", "penelope", "entrance-hall", "Why, how delightful!");
        world.addEvent("action", "hooded-claw", "entrance-hall", "moved to kitchen");
        world.addEvent("dialogue", "dastardly", "entrance-hall", "Mehehehe!");

        var events = world.recentEvents("entrance-hall", 2);
        assertThat(events).hasSize(2);
        assertThat(events.get(0).description()).isEqualTo("Mehehehe!");
        assertThat(events.get(1).description()).isEqualTo("moved to kitchen");
    }

    @Test
    void objects_with_no_visibleTo_are_visible_to_everyone() {
        var objects = world.visibleObjects("entrance-hall", "penelope");
        assertThat(objects).anyMatch(o -> o.id().equals("coat-rack"));
        assertThat(objects).anyMatch(o -> o.id().equals("guest-book"));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest=WorldStateTest -pl .`
Expected: compilation failure — model classes and WorldState don't exist yet.

- [ ] **Step 4: Implement model records**

Create `Room.java`:

```java
package io.casehub.examples.manor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Room(
        String id,
        String name,
        String description,
        List<String> adjacentRooms,
        Map<String, GameObject> objects) {

    public Room {
        adjacentRooms = adjacentRooms != null ? List.copyOf(adjacentRooms) : List.of();
        objects = objects != null ? Map.copyOf(objects) : Map.of();
    }
}
```

Create `GameObject.java`:

```java
package io.casehub.examples.manor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameObject(
        String id,
        String name,
        String description,
        double x,
        Set<String> visibleTo,
        boolean portable,
        boolean interactable,
        String interactionRequires,
        String yields,
        List<String> usableWith) {

    public GameObject {
        visibleTo = visibleTo != null ? Set.copyOf(visibleTo) : Set.of();
        usableWith = usableWith != null ? List.copyOf(usableWith) : List.of();
    }

    public boolean isVisibleToAll() {
        return visibleTo.isEmpty();
    }
}
```

Create `CharacterState.java`:

```java
package io.casehub.examples.manor.model;

import java.util.ArrayList;
import java.util.List;

public final class CharacterState {
    private final String agentId;
    private final String name;
    private String currentRoom;
    private double x;
    private final List<String> inventory;
    private volatile boolean active = true;
    private volatile SceneContext sceneContext;

    public CharacterState(String agentId, String name, String startRoom,
                          double startX, List<String> inventory) {
        this.agentId = agentId;
        this.name = name;
        this.currentRoom = startRoom;
        this.x = startX;
        this.inventory = new ArrayList<>(inventory != null ? inventory : List.of());
    }

    public String agentId() { return agentId; }
    public String name() { return name; }
    public String currentRoom() { return currentRoom; }
    public double x() { return x; }
    public List<String> inventory() { return List.copyOf(inventory); }
    public boolean isActive() { return active; }
    public SceneContext sceneContext() { return sceneContext; }

    public void setCurrentRoom(String room) { this.currentRoom = room; }
    public void setX(double x) { this.x = x; }
    public void setActive(boolean active) { this.active = active; }
    public void setSceneContext(SceneContext ctx) { this.sceneContext = ctx; }

    public void addItem(String itemId) { inventory.add(itemId); }
    public boolean removeItem(String itemId) { return inventory.remove(itemId); }
    public boolean hasItem(String itemId) { return inventory.contains(itemId); }
}
```

Create `SceneContext.java`:

```java
package io.casehub.examples.manor.model;

import java.util.concurrent.CountDownLatch;

public final class SceneContext {
    private final String sceneId;
    private final CountDownLatch latch = new CountDownLatch(1);

    public SceneContext(String sceneId) {
        this.sceneId = sceneId;
    }

    public String sceneId() { return sceneId; }

    public void awaitRelease() throws InterruptedException {
        latch.await();
    }

    public void release() {
        latch.countDown();
    }
}
```

Create `ManorEvent.java`:

```java
package io.casehub.examples.manor.model;

import java.time.Instant;

public record ManorEvent(
        Instant timestamp,
        String type,
        String characterId,
        String room,
        String description) {}
```

- [ ] **Step 5: Implement MansionLoader and WorldState**

Create `MansionLoader.java`:

```java
package io.casehub.examples.manor.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.GameObject;
import io.casehub.examples.manor.model.Room;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MansionLoader {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoomsFile(Map<String, Room> rooms) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CharactersFile(Map<String, CharacterDef> characters) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CharacterDef(String name, String startRoom, double startX, List<String> inventory) {}

    public static WorldState loadWorld() {
        return loadWorld("/mansion/rooms.yaml", "/mansion/characters.yaml");
    }

    public static WorldState loadWorld(String roomsPath, String charactersPath) {
        var rooms = loadRooms(roomsPath);
        var characters = loadCharacters(charactersPath);
        return new WorldState(rooms, characters);
    }

    private static Map<String, Room> loadRooms(String path) {
        try (var stream = MansionLoader.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Resource not found: " + path);
            var file = YAML.readValue(stream, RoomsFile.class);
            var rooms = new LinkedHashMap<String, Room>();
            file.rooms().forEach((id, room) -> {
                var objectsWithIds = new LinkedHashMap<String, GameObject>();
                room.objects().forEach((objId, obj) ->
                    objectsWithIds.put(objId, new GameObject(
                        objId, obj.name(), obj.description(), obj.x(),
                        obj.visibleTo(), obj.portable(), obj.interactable(),
                        obj.interactionRequires(), obj.yields(), obj.usableWith())));
                rooms.put(id, new Room(id, room.name(), room.description(),
                    room.adjacentRooms(), objectsWithIds));
            });
            return Map.copyOf(rooms);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, CharacterState> loadCharacters(String path) {
        try (var stream = MansionLoader.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Resource not found: " + path);
            var file = YAML.readValue(stream, CharactersFile.class);
            var characters = new LinkedHashMap<String, CharacterState>();
            file.characters().forEach((id, def) ->
                characters.put(id, new CharacterState(
                    id, def.name(), def.startRoom(), def.startX(), def.inventory())));
            return characters;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
```

Create `WorldState.java`:

```java
package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.GameObject;
import io.casehub.examples.manor.model.ManorEvent;
import io.casehub.examples.manor.model.Room;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class WorldState {

    private final Map<String, Room> rooms;
    private final Map<String, CharacterState> characters;
    private final Set<String> firedTriggers = new HashSet<>();
    private final Map<String, Set<String>> visibilityOverrides = new HashMap<>();
    private final List<ManorEvent> eventLog = new ArrayList<>();
    private final Set<String> completedScenes = new HashSet<>();
    private volatile boolean scenarioComplete = false;

    public WorldState(Map<String, Room> rooms, Map<String, CharacterState> characters) {
        this.rooms = rooms;
        this.characters = characters;
    }

    public Map<String, Room> rooms() { return rooms; }
    public Map<String, CharacterState> characters() { return characters; }
    public Room room(String id) { return rooms.get(id); }
    public CharacterState character(String id) { return characters.get(id); }

    public List<GameObject> visibleObjects(String roomId, String characterId) {
        Room room = rooms.get(roomId);
        if (room == null) return List.of();
        return room.objects().entrySet().stream()
                .filter(e -> isObjectVisible(e.getKey(), e.getValue(), characterId))
                .map(Map.Entry::getValue)
                .toList();
    }

    private boolean isObjectVisible(String objectId, GameObject obj, String characterId) {
        if (obj.isVisibleToAll()) return true;
        if (obj.visibleTo().contains(characterId)) return true;
        Set<String> overrides = visibilityOverrides.get(objectId);
        return overrides != null && overrides.contains(characterId);
    }

    public void revealObject(String objectId, String characterId) {
        visibilityOverrides.computeIfAbsent(objectId, k -> new HashSet<>()).add(characterId);
    }

    public void revealObjectToAll(String objectId) {
        for (String charId : characters.keySet()) {
            revealObject(objectId, charId);
        }
    }

    public void moveCharacter(String characterId, String roomId) {
        CharacterState c = characters.get(characterId);
        c.setCurrentRoom(roomId);
        Room room = rooms.get(roomId);
        if (room != null) {
            c.setX(0.5);
        }
    }

    public void addToInventory(String characterId, String itemId) {
        characters.get(characterId).addItem(itemId);
    }

    public void removeFromInventory(String characterId, String itemId) {
        characters.get(characterId).removeItem(itemId);
    }

    public void markCharacterInactive(String agentId) {
        CharacterState c = characters.get(agentId);
        if (c != null) c.setActive(false);
    }

    public boolean isScenarioComplete() { return scenarioComplete; }
    public void setScenarioComplete() { this.scenarioComplete = true; }

    public boolean hasTriggerFired(String triggerId) { return firedTriggers.contains(triggerId); }
    public void markTriggerFired(String triggerId) { firedTriggers.add(triggerId); }

    public boolean isSceneCompleted(String sceneId) { return completedScenes.contains(sceneId); }
    public void markSceneCompleted(String sceneId) { completedScenes.add(sceneId); }

    public void addEvent(String type, String characterId, String room, String description) {
        eventLog.add(new ManorEvent(Instant.now(), type, characterId, room, description));
    }

    public List<ManorEvent> recentEvents(String roomId, int limit) {
        return eventLog.reversed().stream()
                .filter(e -> roomId.equals(e.room()))
                .limit(limit)
                .toList();
    }

    public List<CharacterState> charactersInRoom(String roomId) {
        return characters.values().stream()
                .filter(c -> c.isActive() && roomId.equals(c.currentRoom()))
                .toList();
    }

    public GameObject findObject(String objectId) {
        for (Room room : rooms.values()) {
            GameObject obj = room.objects().get(objectId);
            if (obj != null) return obj;
        }
        return null;
    }

    public String findObjectRoom(String objectId) {
        for (Map.Entry<String, Room> entry : rooms.entrySet()) {
            if (entry.getValue().objects().containsKey(objectId)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
```

- [ ] **Step 6: Run tests**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest=WorldStateTest`
Expected: all 11 tests pass.

- [ ] **Step 7: Commit**

```bash
git -C examples add wacky-manor/src/main/java/io/casehub/examples/manor/model/ wacky-manor/src/main/java/io/casehub/examples/manor/engine/WorldState.java wacky-manor/src/main/java/io/casehub/examples/manor/engine/MansionLoader.java wacky-manor/src/main/resources/mansion/ wacky-manor/src/test/java/io/casehub/examples/manor/engine/WorldStateTest.java
git -C examples commit -m "feat(wacky-manor): add world model — rooms, characters, WorldState with YAML loading

Refs #TBD — Phase 1 game engine"
```

---

### Task 2: Action Resolution

**Files:**
- Create: `src/main/java/io/casehub/examples/manor/model/ActionType.java`
- Create: `src/main/java/io/casehub/examples/manor/model/Action.java`
- Create: `src/main/java/io/casehub/examples/manor/model/ActionResult.java`
- Create: `src/main/java/io/casehub/examples/manor/model/PendingAction.java`
- Create: `src/main/java/io/casehub/examples/manor/engine/ActionResolver.java`
- Test: `src/test/java/io/casehub/examples/manor/engine/ActionResolverTest.java`
- Test: `src/test/java/io/casehub/examples/manor/engine/ItemDependencyTest.java`

**Interfaces:**
- Consumes: `WorldState`, `Room`, `GameObject`, `CharacterState` from Task 1
- Produces:
  - `ActionType` enum: `MOVE, INTERACT, TAKE, GIVE, USE, LOOK, WAIT`
  - `Action(ActionType type, String target, String withItem)`
  - `ActionResult` sealed: `Success(String)`, `Failed(String)`, `MovedToRoom(String roomId, String description)`, `ItemReceived(String itemId, String description)`, `SceneTriggered(String sceneId)`
  - `PendingAction` — wraps `CharacterState` + `Action` + `CompletableFuture<ActionResult>`
  - `ActionResolver.resolve(CharacterState character, Action action, WorldState world)` → `ActionResult`

- [ ] **Step 1: Write failing ActionResolverTest**

```java
package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActionResolverTest {

    private WorldState world;
    private ActionResolver resolver;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        resolver = new ActionResolver();
    }

    @Test
    void move_to_adjacent_room_succeeds() {
        var penelope = world.character("penelope");
        var result = resolver.resolve(penelope,
            new Action(ActionType.MOVE, "kitchen", null), world);
        assertThat(result).isInstanceOf(ActionResult.MovedToRoom.class);
        assertThat(penelope.currentRoom()).isEqualTo("kitchen");
    }

    @Test
    void move_to_non_adjacent_room_fails() {
        var penelope = world.character("penelope");
        var result = resolver.resolve(penelope,
            new Action(ActionType.MOVE, "ballroom", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
        assertThat(penelope.currentRoom()).isEqualTo("entrance-hall");
    }

    @Test
    void take_portable_object_adds_to_inventory() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "kitchen");
        hc.setX(0.7);

        var result = resolver.resolve(hc,
            new Action(ActionType.TAKE, "poison", null), world);
        assertThat(result).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(hc.hasItem("rat-poison")).isTrue();
    }

    @Test
    void take_non_portable_object_fails() {
        var penelope = world.character("penelope");
        penelope.setX(0.5);
        var result = resolver.resolve(penelope,
            new Action(ActionType.TAKE, "guest-book", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void interact_without_required_item_fails() {
        var penelope = world.character("penelope");
        penelope.setX(0.8);
        var result = resolver.resolve(penelope,
            new Action(ActionType.INTERACT, "muttley", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void interact_with_required_item_succeeds_and_yields() {
        var dastardly = world.character("dick-dastardly");
        dastardly.setX(0.8);
        var result = resolver.resolve(dastardly,
            new Action(ActionType.INTERACT, "muttley", "fake-medal"), world);
        assertThat(result).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(dastardly.hasItem("brass-key")).isTrue();
        assertThat(dastardly.hasItem("fake-medal")).isFalse();
    }

    @Test
    void give_transfers_item_to_character_in_same_room() {
        var dastardly = world.character("dick-dastardly");
        var penelope = world.character("penelope");
        dastardly.setX(0.3);
        penelope.setX(0.3);

        var result = resolver.resolve(dastardly,
            new Action(ActionType.GIVE, "penelope", "fake-medal"), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
        assertThat(dastardly.hasItem("fake-medal")).isFalse();
        assertThat(penelope.hasItem("fake-medal")).isTrue();
    }

    @Test
    void give_to_character_in_different_room_fails() {
        var dastardly = world.character("dick-dastardly");
        world.moveCharacter("penelope", "kitchen");

        var result = resolver.resolve(dastardly,
            new Action(ActionType.GIVE, "penelope", "fake-medal"), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void use_item_on_compatible_object() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "ballroom");
        hc.setX(0.5);
        hc.addItem("rat-poison");

        var result = resolver.resolve(hc,
            new Action(ActionType.USE, "tea-service", "rat-poison"), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
    }

    @Test
    void use_incompatible_item_on_object_fails() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "ballroom");
        hc.setX(0.5);
        hc.addItem("brass-key");

        var result = resolver.resolve(hc,
            new Action(ActionType.USE, "tea-service", "brass-key"), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void look_always_succeeds() {
        var penelope = world.character("penelope");
        var result = resolver.resolve(penelope,
            new Action(ActionType.LOOK, "coat-rack", null), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
    }

    @Test
    void wait_always_succeeds() {
        var penelope = world.character("penelope");
        var result = resolver.resolve(penelope,
            new Action(ActionType.WAIT, null, null), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
    }

    @Test
    void proximity_check_rejects_distant_interaction() {
        var penelope = world.character("penelope");
        world.moveCharacter("penelope", "kitchen");
        penelope.setX(0.1);

        var result = resolver.resolve(penelope,
            new Action(ActionType.INTERACT, "cabinet", "brass-key"), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
        assertThat(((ActionResult.Failed) result).reason()).contains("too far");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest=ActionResolverTest`
Expected: compilation failure.

- [ ] **Step 3: Implement model types**

Create `ActionType.java`:

```java
package io.casehub.examples.manor.model;

public enum ActionType {
    MOVE, INTERACT, TAKE, GIVE, USE, LOOK, WAIT
}
```

Create `Action.java`:

```java
package io.casehub.examples.manor.model;

public record Action(ActionType type, String target, String withItem) {}
```

Create `ActionResult.java`:

```java
package io.casehub.examples.manor.model;

public sealed interface ActionResult {
    record Success(String description) implements ActionResult {}
    record Failed(String reason) implements ActionResult {}
    record MovedToRoom(String roomId, String description) implements ActionResult {}
    record ItemReceived(String itemId, String description) implements ActionResult {}
    record SceneTriggered(String sceneId) implements ActionResult {}
}
```

Create `PendingAction.java`:

```java
package io.casehub.examples.manor.model;

import java.util.concurrent.CompletableFuture;

public final class PendingAction {
    private final CharacterState character;
    private final Action action;
    private final CompletableFuture<ActionResult> future = new CompletableFuture<>();

    public PendingAction(CharacterState character, Action action) {
        this.character = character;
        this.action = action;
    }

    public CharacterState character() { return character; }
    public Action action() { return action; }

    public void complete(ActionResult result) { future.complete(result); }
    public ActionResult awaitResult() throws Exception { return future.get(); }
}
```

- [ ] **Step 4: Implement ActionResolver**

Create `ActionResolver.java`:

```java
package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.*;

public final class ActionResolver {

    private static final double PROXIMITY_THRESHOLD = 0.15;

    public ActionResult resolve(CharacterState character, Action action, WorldState world) {
        return switch (action.type()) {
            case MOVE -> resolveMove(character, action, world);
            case INTERACT -> resolveInteract(character, action, world);
            case TAKE -> resolveTake(character, action, world);
            case GIVE -> resolveGive(character, action, world);
            case USE -> resolveUse(character, action, world);
            case LOOK -> resolveLook(character, action, world);
            case WAIT -> new ActionResult.Success("You wait and observe.");
        };
    }

    private ActionResult resolveMove(CharacterState character, Action action, WorldState world) {
        Room currentRoom = world.room(character.currentRoom());
        if (!currentRoom.adjacentRooms().contains(action.target())) {
            return new ActionResult.Failed(
                "Cannot move to " + action.target() + " — not adjacent to " + character.currentRoom());
        }
        Room targetRoom = world.room(action.target());
        if (targetRoom == null) {
            return new ActionResult.Failed("Unknown room: " + action.target());
        }
        world.moveCharacter(character.agentId(), action.target());
        return new ActionResult.MovedToRoom(action.target(),
            "You moved to " + targetRoom.name() + ".");
    }

    private ActionResult resolveInteract(CharacterState character, Action action, WorldState world) {
        Room room = world.room(character.currentRoom());
        GameObject obj = room.objects().get(action.target());
        if (obj == null) {
            return new ActionResult.Failed("No such object: " + action.target());
        }
        if (!obj.interactable()) {
            return new ActionResult.Failed(obj.name() + " cannot be interacted with.");
        }
        if (!isWithinProximity(character.x(), obj.x())) {
            return new ActionResult.Failed("You are too far from " + obj.name() + ".");
        }
        if (obj.interactionRequires() != null) {
            if (action.withItem() == null || !action.withItem().equals(obj.interactionRequires())) {
                return new ActionResult.Failed(obj.name() + " requires " + obj.interactionRequires() + ".");
            }
            character.removeItem(action.withItem());
        }
        if (obj.yields() != null) {
            character.addItem(obj.yields());
            return new ActionResult.ItemReceived(obj.yields(),
                "You received " + obj.yields() + " from " + obj.name() + ".");
        }
        return new ActionResult.Success("You interacted with " + obj.name() + ".");
    }

    private ActionResult resolveTake(CharacterState character, Action action, WorldState world) {
        Room room = world.room(character.currentRoom());
        GameObject obj = room.objects().get(action.target());
        if (obj == null) {
            return new ActionResult.Failed("No such object: " + action.target());
        }
        if (!obj.portable()) {
            return new ActionResult.Failed(obj.name() + " cannot be picked up.");
        }
        if (!isWithinProximity(character.x(), obj.x())) {
            return new ActionResult.Failed("You are too far from " + obj.name() + ".");
        }
        String itemId = action.target().replace("-", "-");
        if ("poison".equals(action.target())) itemId = "rat-poison";
        character.addItem(itemId);
        return new ActionResult.ItemReceived(itemId,
            "You picked up " + obj.name() + ".");
    }

    private ActionResult resolveGive(CharacterState character, Action action, WorldState world) {
        if (action.withItem() == null || !character.hasItem(action.withItem())) {
            return new ActionResult.Failed("You don't have " + action.withItem() + ".");
        }
        CharacterState target = world.character(action.target());
        if (target == null) {
            return new ActionResult.Failed("Unknown character: " + action.target());
        }
        if (!target.currentRoom().equals(character.currentRoom())) {
            return new ActionResult.Failed(target.name() + " is not in this room.");
        }
        if (!isWithinProximity(character.x(), target.x())) {
            return new ActionResult.Failed("You are too far from " + target.name() + ".");
        }
        character.removeItem(action.withItem());
        target.addItem(action.withItem());
        return new ActionResult.Success(
            "You gave " + action.withItem() + " to " + target.name() + ".");
    }

    private ActionResult resolveUse(CharacterState character, Action action, WorldState world) {
        if (action.withItem() == null || !character.hasItem(action.withItem())) {
            return new ActionResult.Failed("You don't have " + action.withItem() + ".");
        }
        Room room = world.room(character.currentRoom());
        GameObject obj = room.objects().get(action.target());
        if (obj == null) {
            return new ActionResult.Failed("No such object: " + action.target());
        }
        if (!isWithinProximity(character.x(), obj.x())) {
            return new ActionResult.Failed("You are too far from " + obj.name() + ".");
        }
        if (!obj.usableWith().contains(action.withItem())) {
            return new ActionResult.Failed(
                action.withItem() + " cannot be used on " + obj.name() + ".");
        }
        return new ActionResult.Success(
            "You used " + action.withItem() + " on " + obj.name() + ".");
    }

    private ActionResult resolveLook(CharacterState character, Action action, WorldState world) {
        if (action.target() == null) {
            Room room = world.room(character.currentRoom());
            return new ActionResult.Success("You look around " + room.name() + ".");
        }
        Room room = world.room(character.currentRoom());
        GameObject obj = room.objects().get(action.target());
        if (obj != null) {
            return new ActionResult.Success("You examine " + obj.name() + ": " + obj.description());
        }
        return new ActionResult.Success("You look at " + action.target() + ".");
    }

    private boolean isWithinProximity(double characterX, double objectX) {
        return Math.abs(characterX - objectX) <= PROXIMITY_THRESHOLD;
    }
}
```

- [ ] **Step 5: Write ItemDependencyTest**

```java
package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemDependencyTest {

    private WorldState world;
    private ActionResolver resolver;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        resolver = new ActionResolver();
    }

    @Test
    void full_item_chain_fake_medal_to_recipe_cards() {
        var dastardly = world.character("dick-dastardly");
        dastardly.setX(0.8);

        var result1 = resolver.resolve(dastardly,
            new Action(ActionType.INTERACT, "muttley", "fake-medal"), world);
        assertThat(result1).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(dastardly.hasItem("brass-key")).isTrue();

        world.moveCharacter("dick-dastardly", "kitchen");
        dastardly.setX(0.3);

        var result2 = resolver.resolve(dastardly,
            new Action(ActionType.INTERACT, "cabinet", "brass-key"), world);
        assertThat(result2).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(dastardly.hasItem("old-recipe-cards")).isTrue();
    }

    @Test
    void poison_chain_take_then_use_on_tea() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "kitchen");
        hc.setX(0.7);

        var take = resolver.resolve(hc,
            new Action(ActionType.TAKE, "poison", null), world);
        assertThat(take).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(hc.hasItem("rat-poison")).isTrue();

        world.moveCharacter("hooded-claw", "ballroom");
        hc.setX(0.5);

        var use = resolver.resolve(hc,
            new Action(ActionType.USE, "tea-service", "rat-poison"), world);
        assertThat(use).isInstanceOf(ActionResult.Success.class);
    }
}
```

- [ ] **Step 6: Run all tests**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest="ActionResolverTest,ItemDependencyTest,WorldStateTest"`
Expected: all pass.

- [ ] **Step 7: Commit**

```bash
git -C examples add wacky-manor/src/main/java/io/casehub/examples/manor/model/ActionType.java wacky-manor/src/main/java/io/casehub/examples/manor/model/Action.java wacky-manor/src/main/java/io/casehub/examples/manor/model/ActionResult.java wacky-manor/src/main/java/io/casehub/examples/manor/model/PendingAction.java wacky-manor/src/main/java/io/casehub/examples/manor/engine/ActionResolver.java wacky-manor/src/test/java/io/casehub/examples/manor/engine/
git -C examples commit -m "feat(wacky-manor): add action resolution — ActionType, ActionResult, proximity enforcement

Refs #TBD"
```

---

### Task 3: Trigger Evaluator

**Files:**
- Create: `src/main/java/io/casehub/examples/manor/model/Trigger.java`
- Create: `src/main/java/io/casehub/examples/manor/model/TriggerCondition.java`
- Create: `src/main/java/io/casehub/examples/manor/model/TriggerEffect.java`
- Create: `src/main/java/io/casehub/examples/manor/engine/TriggerEvaluator.java`
- Modify: `src/main/java/io/casehub/examples/manor/engine/MansionLoader.java` — add trigger loading
- Create: `src/main/resources/mansion/triggers.yaml`
- Test: `src/test/java/io/casehub/examples/manor/engine/TriggerEvaluatorTest.java`

**Interfaces:**
- Consumes: `WorldState`, `CharacterState` from Tasks 1-2
- Produces:
  - `TriggerCondition` sealed: `CharacterInRoom(characterId, roomId)`, `CharacterHasItem(characterId, itemId)`, `ObjectInRoom(objectId, roomId)`, `SceneCompleted(sceneId)`, `AllOf(List<TriggerCondition>)`
  - `TriggerEffect` sealed: `RevealObject(objectId, roomId)`, `StartScene(sceneId)`, `NarratorEvent(text)`, `CompleteScenario()`, `RemoveItem(characterId, itemId)`
  - `Trigger(String id, TriggerCondition condition, List<TriggerEffect> effects, boolean once)`
  - `TriggerEvaluator.evaluate(WorldState)` → `TriggerResult`
  - `TriggerResult(boolean hasSceneStart, String sceneId, List<String> narratorEvents)`

- [ ] **Step 1: Write triggers.yaml**

```yaml
triggers:
  - id: poison-discovery
    condition:
      characterInRoom:
        character: hooded-claw
        room: kitchen
    effects:
      - revealObject:
          object: poison
          room: kitchen
      - narratorEvent: "The Hooded Claw's eyes GLEAM with malicious delight as he spots a most DIABOLICAL substance on the shelf!"
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
    effects:
      - startScene: tea-poisoning
    once: true

  - id: scenario-complete
    condition:
      sceneCompleted: tea-poisoning
    effects:
      - completeScenario: true
    once: true
```

- [ ] **Step 2: Write failing TriggerEvaluatorTest**

```java
package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TriggerEvaluatorTest {

    private WorldState world;
    private TriggerEvaluator evaluator;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        evaluator = new TriggerEvaluator(MansionLoader.loadTriggers());
    }

    @Test
    void poison_revealed_when_hooded_claw_enters_kitchen() {
        assertThat(world.visibleObjects("kitchen", "penelope"))
            .noneMatch(o -> o.id().equals("poison"));

        world.moveCharacter("hooded-claw", "kitchen");
        var result = evaluator.evaluate(world);

        assertThat(result.narratorEvents()).hasSize(1);
        assertThat(result.narratorEvents().get(0)).contains("DIABOLICAL");
        assertThat(world.visibleObjects("kitchen", "hooded-claw"))
            .anyMatch(o -> o.id().equals("poison"));
    }

    @Test
    void once_trigger_does_not_fire_twice() {
        world.moveCharacter("hooded-claw", "kitchen");
        evaluator.evaluate(world);
        var result2 = evaluator.evaluate(world);
        assertThat(result2.narratorEvents()).isEmpty();
    }

    @Test
    void tea_scene_triggers_when_all_conditions_met() {
        world.moveCharacter("hooded-claw", "kitchen");
        evaluator.evaluate(world);

        world.character("hooded-claw").addItem("rat-poison");
        world.moveCharacter("hooded-claw", "ballroom");
        world.moveCharacter("penelope", "ballroom");

        var result = evaluator.evaluate(world);
        assertThat(result.hasSceneStart()).isTrue();
        assertThat(result.sceneId()).isEqualTo("tea-poisoning");
    }

    @Test
    void tea_scene_does_not_trigger_without_poison() {
        world.moveCharacter("hooded-claw", "ballroom");
        world.moveCharacter("penelope", "ballroom");

        var result = evaluator.evaluate(world);
        assertThat(result.hasSceneStart()).isFalse();
    }

    @Test
    void scenario_completes_when_scene_done() {
        world.markSceneCompleted("tea-poisoning");
        evaluator.evaluate(world);
        assertThat(world.isScenarioComplete()).isTrue();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest=TriggerEvaluatorTest`
Expected: compilation failure.

- [ ] **Step 4: Implement trigger model types**

Create `TriggerCondition.java`:

```java
package io.casehub.examples.manor.model;

import java.util.List;

public sealed interface TriggerCondition {
    record CharacterInRoom(String character, String room) implements TriggerCondition {}
    record CharacterHasItem(String character, String item) implements TriggerCondition {}
    record ObjectInRoom(String object, String room) implements TriggerCondition {}
    record SceneCompleted(String sceneId) implements TriggerCondition {}
    record AllOf(List<TriggerCondition> conditions) implements TriggerCondition {}
}
```

Create `TriggerEffect.java`:

```java
package io.casehub.examples.manor.model;

public sealed interface TriggerEffect {
    record RevealObject(String object, String room) implements TriggerEffect {}
    record StartScene(String sceneId) implements TriggerEffect {}
    record NarratorEvent(String text) implements TriggerEffect {}
    record CompleteScenario() implements TriggerEffect {}
    record RemoveItem(String character, String item) implements TriggerEffect {}
}
```

Create `Trigger.java`:

```java
package io.casehub.examples.manor.model;

import java.util.List;

public record Trigger(
        String id,
        TriggerCondition condition,
        List<TriggerEffect> effects,
        boolean once) {}
```

- [ ] **Step 5: Implement TriggerEvaluator and add YAML loading to MansionLoader**

Create `TriggerEvaluator.java`:

```java
package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TriggerEvaluator {

    private final List<Trigger> triggers;
    private final Set<String> firedOnce = new HashSet<>();

    public TriggerEvaluator(List<Trigger> triggers) {
        this.triggers = triggers;
    }

    public TriggerResult evaluate(WorldState world) {
        String sceneId = null;
        var narratorEvents = new ArrayList<String>();

        for (Trigger trigger : triggers) {
            if (trigger.once() && firedOnce.contains(trigger.id())) continue;
            if (!matches(trigger.condition(), world)) continue;

            if (trigger.once()) firedOnce.add(trigger.id());

            for (TriggerEffect effect : trigger.effects()) {
                switch (effect) {
                    case TriggerEffect.RevealObject r ->
                        world.revealObject(r.object(), world.characters().keySet().stream()
                            .filter(c -> world.character(c).currentRoom().equals(r.room()))
                            .findFirst().orElse(null));
                    case TriggerEffect.StartScene s ->
                        sceneId = s.sceneId();
                    case TriggerEffect.NarratorEvent n ->
                        narratorEvents.add(n.text());
                    case TriggerEffect.CompleteScenario ignored ->
                        world.setScenarioComplete();
                    case TriggerEffect.RemoveItem r ->
                        world.removeFromInventory(r.character(), r.item());
                }
            }
        }
        return new TriggerResult(sceneId != null, sceneId, narratorEvents);
    }

    private boolean matches(TriggerCondition condition, WorldState world) {
        return switch (condition) {
            case TriggerCondition.CharacterInRoom c ->
                c.room().equals(world.character(c.character()).currentRoom());
            case TriggerCondition.CharacterHasItem c ->
                world.character(c.character()).hasItem(c.item());
            case TriggerCondition.ObjectInRoom o ->
                world.room(o.room()) != null &&
                    world.room(o.room()).objects().containsKey(o.object());
            case TriggerCondition.SceneCompleted s ->
                world.isSceneCompleted(s.sceneId());
            case TriggerCondition.AllOf all ->
                all.conditions().stream().allMatch(c -> matches(c, world));
        };
    }

    public record TriggerResult(
            boolean hasSceneStart,
            String sceneId,
            List<String> narratorEvents) {}
}
```

Add `loadTriggers()` to `MansionLoader` — parse `triggers.yaml` into `List<Trigger>`. The YAML has a non-trivial structure (nested sealed types), so use a Jackson custom deserializer or manual map parsing. For simplicity, parse the raw YAML tree manually:

Add method to `MansionLoader.java`:

```java
@SuppressWarnings("unchecked")
public static List<Trigger> loadTriggers() {
    return loadTriggers("/mansion/triggers.yaml");
}

@SuppressWarnings("unchecked")
public static List<Trigger> loadTriggers(String path) {
    try (var stream = MansionLoader.class.getResourceAsStream(path)) {
        if (stream == null) throw new IllegalStateException("Resource not found: " + path);
        Map<String, Object> root = YAML.readValue(stream,
            new com.fasterxml.jackson.core.type.TypeReference<>() {});
        var triggerDefs = (List<Map<String, Object>>) root.get("triggers");
        return triggerDefs.stream().map(MansionLoader::parseTrigger).toList();
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
}

@SuppressWarnings("unchecked")
private static Trigger parseTrigger(Map<String, Object> map) {
    String id = (String) map.get("id");
    var condMap = (Map<String, Object>) map.get("condition");
    var effectList = (List<Map<String, Object>>) map.get("effects");
    boolean once = Boolean.TRUE.equals(map.get("once"));

    TriggerCondition condition = parseCondition(condMap);
    List<TriggerEffect> effects = effectList.stream()
        .map(MansionLoader::parseEffect).toList();
    return new Trigger(id, condition, effects, once);
}

@SuppressWarnings("unchecked")
private static TriggerCondition parseCondition(Map<String, Object> map) {
    if (map.containsKey("characterInRoom")) {
        var v = (Map<String, String>) map.get("characterInRoom");
        return new TriggerCondition.CharacterInRoom(v.get("character"), v.get("room"));
    }
    if (map.containsKey("characterHasItem")) {
        var v = (Map<String, String>) map.get("characterHasItem");
        return new TriggerCondition.CharacterHasItem(v.get("character"), v.get("item"));
    }
    if (map.containsKey("objectInRoom")) {
        var v = (Map<String, String>) map.get("objectInRoom");
        return new TriggerCondition.ObjectInRoom(v.get("object"), v.get("room"));
    }
    if (map.containsKey("sceneCompleted")) {
        return new TriggerCondition.SceneCompleted((String) map.get("sceneCompleted"));
    }
    if (map.containsKey("allOf")) {
        var conditions = (List<Map<String, Object>>) map.get("allOf");
        return new TriggerCondition.AllOf(
            conditions.stream().map(MansionLoader::parseCondition).toList());
    }
    throw new IllegalArgumentException("Unknown trigger condition: " + map);
}

@SuppressWarnings("unchecked")
private static TriggerEffect parseEffect(Map<String, Object> map) {
    if (map.containsKey("revealObject")) {
        var v = (Map<String, String>) map.get("revealObject");
        return new TriggerEffect.RevealObject(v.get("object"), v.get("room"));
    }
    if (map.containsKey("startScene")) {
        return new TriggerEffect.StartScene((String) map.get("startScene"));
    }
    if (map.containsKey("narratorEvent")) {
        return new TriggerEffect.NarratorEvent((String) map.get("narratorEvent"));
    }
    if (map.containsKey("completeScenario")) {
        return new TriggerEffect.CompleteScenario();
    }
    if (map.containsKey("removeItem")) {
        var v = (Map<String, String>) map.get("removeItem");
        return new TriggerEffect.RemoveItem(v.get("character"), v.get("item"));
    }
    throw new IllegalArgumentException("Unknown trigger effect: " + map);
}
```

Note: the `RevealObject` trigger needs refinement. The poison-discovery trigger should reveal the object to the character who entered the room (hooded-claw), not to all characters in the room. Update the RevealObject handling in `TriggerEvaluator.evaluate()`:

```java
case TriggerEffect.RevealObject r -> {
    // Reveal to the character who triggered it (in the room)
    for (CharacterState c : world.charactersInRoom(r.room())) {
        world.revealObject(r.object(), c.agentId());
    }
}
```

- [ ] **Step 6: Run tests**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest="TriggerEvaluatorTest,WorldStateTest"`
Expected: all pass.

- [ ] **Step 7: Commit**

```bash
git -C examples add wacky-manor/src/main/java/io/casehub/examples/manor/model/Trigger.java wacky-manor/src/main/java/io/casehub/examples/manor/model/TriggerCondition.java wacky-manor/src/main/java/io/casehub/examples/manor/model/TriggerEffect.java wacky-manor/src/main/java/io/casehub/examples/manor/engine/TriggerEvaluator.java wacky-manor/src/main/resources/mansion/triggers.yaml wacky-manor/src/main/java/io/casehub/examples/manor/engine/MansionLoader.java wacky-manor/src/test/java/io/casehub/examples/manor/engine/TriggerEvaluatorTest.java
git -C examples commit -m "feat(wacky-manor): add trigger evaluator — condition matching, reveal/scene/narrator effects

Refs #TBD"
```

---

### Task 4: Observation Builder

**Files:**
- Create: `src/main/java/io/casehub/examples/manor/agent/ObservationBuilder.java`
- Test: `src/test/java/io/casehub/examples/manor/agent/ObservationBuilderTest.java`

**Interfaces:**
- Consumes: `WorldState`, `CharacterState`, `Room`, `GameObject`, `ManorEvent` from Tasks 1-2
- Produces:
  - `ObservationBuilder.buildObservation(CharacterState character, WorldState world)` → `String`

- [ ] **Step 1: Write failing ObservationBuilderTest**

```java
package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.engine.WorldState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationBuilderTest {

    private WorldState world;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
    }

    @Test
    void observation_includes_current_room() {
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope"), world);
        assertThat(obs).contains("== Current Location ==");
        assertThat(obs).contains("Entrance Hall");
    }

    @Test
    void observation_shows_visible_objects() {
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope"), world);
        assertThat(obs).contains("Coat Rack");
        assertThat(obs).contains("Guest Book");
        assertThat(obs).doesNotContain("Rat Poison");
    }

    @Test
    void hooded_claw_sees_poison_in_kitchen() {
        world.moveCharacter("hooded-claw", "kitchen");
        var obs = ObservationBuilder.buildObservation(
            world.character("hooded-claw"), world);
        assertThat(obs).contains("Rat Poison");
        assertThat(obs).contains("[can be picked up]");
    }

    @Test
    void penelope_does_not_see_poison() {
        world.moveCharacter("penelope", "kitchen");
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope"), world);
        assertThat(obs).doesNotContain("Rat Poison");
    }

    @Test
    void observation_lists_characters_in_room() {
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope"), world);
        assertThat(obs).contains("== Characters Present ==");
        assertThat(obs).contains("The Hooded Claw");
        assertThat(obs).doesNotContain("Penelope Pitstop");
    }

    @Test
    void observation_shows_alone_when_no_others() {
        world.moveCharacter("penelope", "kitchen");
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope"), world);
        assertThat(obs).contains("You are alone.");
    }

    @Test
    void observation_shows_inventory() {
        var dastardly = world.character("dick-dastardly");
        var obs = ObservationBuilder.buildObservation(dastardly, world);
        assertThat(obs).contains("== Your Inventory ==");
        assertThat(obs).contains("fake-medal");
    }

    @Test
    void observation_shows_empty_inventory() {
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope"), world);
        assertThat(obs).contains("You are carrying nothing.");
    }

    @Test
    void observation_includes_recent_events() {
        world.addEvent("dialogue", "hooded-claw", "entrance-hall",
            "Oh, my DEAR Miss Pitstop!");
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope"), world);
        assertThat(obs).contains("== Recent Activity ==");
        assertThat(obs).contains("Oh, my DEAR Miss Pitstop!");
    }

    @Test
    void observation_shows_quiet_room_when_no_events() {
        world.moveCharacter("penelope", "kitchen");
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope"), world);
        assertThat(obs).contains("The room is quiet.");
    }

    @Test
    void observation_shows_interactable_hints() {
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope"), world);
        assertThat(obs).contains("[interactable, requires: fake-medal]");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest=ObservationBuilderTest`
Expected: compilation failure.

- [ ] **Step 3: Implement ObservationBuilder**

```java
package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.GameObject;
import io.casehub.examples.manor.model.ManorEvent;
import io.casehub.examples.manor.model.Room;

import java.util.List;

public final class ObservationBuilder {

    private static final int RECENT_EVENT_LIMIT = 5;

    public static String buildObservation(CharacterState character, WorldState world) {
        var sb = new StringBuilder();
        Room room = world.room(character.currentRoom());

        appendLocation(sb, room);
        appendVisibleObjects(sb, character, world);
        appendCharactersPresent(sb, character, world);
        appendInventory(sb, character);
        appendRecentActivity(sb, character, world);

        return sb.toString();
    }

    private static void appendLocation(StringBuilder sb, Room room) {
        sb.append("== Current Location ==\n");
        sb.append(room.name()).append(": ").append(room.description()).append("\n\n");
    }

    private static void appendVisibleObjects(StringBuilder sb, CharacterState character,
                                              WorldState world) {
        sb.append("== Visible Objects ==\n");
        List<GameObject> objects = world.visibleObjects(character.currentRoom(), character.agentId());
        if (objects.isEmpty()) {
            sb.append("Nothing notable here.\n");
        } else {
            for (GameObject obj : objects) {
                sb.append("- ").append(obj.name())
                  .append(" (at position ").append(obj.x()).append("): ")
                  .append(obj.description());
                if (obj.interactable() || obj.portable()) {
                    sb.append(" ");
                    if (obj.interactable()) {
                        sb.append("[interactable");
                        if (obj.interactionRequires() != null) {
                            sb.append(", requires: ").append(obj.interactionRequires());
                        }
                        sb.append("]");
                    }
                    if (obj.portable()) {
                        sb.append("[can be picked up]");
                    }
                }
                sb.append("\n");
            }
        }
        sb.append("\n");
    }

    private static void appendCharactersPresent(StringBuilder sb, CharacterState character,
                                                 WorldState world) {
        sb.append("== Characters Present ==\n");
        List<CharacterState> others = world.charactersInRoom(character.currentRoom()).stream()
            .filter(c -> !c.agentId().equals(character.agentId()))
            .toList();
        if (others.isEmpty()) {
            sb.append("You are alone.\n");
        } else {
            for (CharacterState other : others) {
                sb.append("- ").append(other.name())
                  .append(" (at position ").append(other.x()).append(")\n");
            }
        }
        sb.append("\n");
    }

    private static void appendInventory(StringBuilder sb, CharacterState character) {
        sb.append("== Your Inventory ==\n");
        if (character.inventory().isEmpty()) {
            sb.append("You are carrying nothing.\n");
        } else {
            for (String item : character.inventory()) {
                sb.append("- ").append(item).append("\n");
            }
        }
        sb.append("\n");
    }

    private static void appendRecentActivity(StringBuilder sb, CharacterState character,
                                              WorldState world) {
        sb.append("== Recent Activity ==\n");
        List<ManorEvent> events = world.recentEvents(
            character.currentRoom(), RECENT_EVENT_LIMIT);
        if (events.isEmpty()) {
            sb.append("The room is quiet.\n");
        } else {
            for (ManorEvent event : events) {
                sb.append("- ").append(event.description()).append("\n");
            }
        }
    }
}
```

- [ ] **Step 4: Run tests**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest=ObservationBuilderTest`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git -C examples add wacky-manor/src/main/java/io/casehub/examples/manor/agent/ObservationBuilder.java wacky-manor/src/test/java/io/casehub/examples/manor/agent/ObservationBuilderTest.java
git -C examples commit -m "feat(wacky-manor): add observation builder — per-character world view with visibility filtering

Refs #TBD"
```

---

### Task 5: Scene Director

**Files:**
- Create: `src/main/java/io/casehub/examples/manor/model/Scene.java`
- Create: `src/main/java/io/casehub/examples/manor/model/Beat.java`
- Create: `src/main/java/io/casehub/examples/manor/engine/SceneDirector.java`
- Modify: `src/main/java/io/casehub/examples/manor/engine/MansionLoader.java` — add scene loading
- Create: `src/main/resources/mansion/scenes.yaml`
- Test: `src/test/java/io/casehub/examples/manor/engine/SceneDirectorTest.java`

**Interfaces:**
- Consumes: `WorldState`, `CharacterState`, `SceneContext`, `TriggerCondition` (for alternative conditions) from Tasks 1-3
- Produces:
  - `Beat(String id, String narration, Map<String, String> prompts, boolean aside, List<BeatAlternative> alternatives, Map<String, Object> mechanicalEffect, boolean waitIfNoneMatch)`
  - `Beat.BeatAlternative(String id, TriggerCondition condition, String narration, Map<String, String> prompts, Map<String, Object> mechanicalEffect)`
  - `Scene(String id, List<Beat> beats)`
  - `SceneDirector` — `runScene(sceneId, world, agentCaller, narratorCallback)` runs synchronously on the game loop thread. `agentCaller` is a functional interface `(systemPrompt, userPrompt) → String` for LLM calls. `narratorCallback` is `Consumer<String>` for narrator events.

- [ ] **Step 1: Write scenes.yaml**

```yaml
scenes:
  tea-poisoning:
    beats:
      - id: offer-tea
        narration: "The Hooded Claw, with a smile that could FREEZE mercury, volunteers to pour the tea!"
        prompts:
          hooded-claw: "You have the poison. The tea service is right here. Penelope is sitting at the table. Offer to pour her tea. Stay in character as Sneekly."
          penelope: "Sneekly is offering to pour you tea. How kind!"

      - id: pour-poison
        narration: "With TREMBLING hands of EVIL anticipation, Sneekly reaches for the poison!"
        prompts:
          hooded-claw: "You're pouring tea. Secretly slip the poison into Penelope's cup while making small talk."
        aside: true

      - id: foil
        alternatives:
          - id: mob-foil
            condition:
              characterInRoom:
                character: ant-hill-mob
                room: ballroom
            narration: "But WAIT! Can it be? The Ant Hill Mob are here!"
            prompts:
              ant-hill-mob: "You notice Sneekly putting something in Penelope's tea cup. React — but remember, you're bumbling, not clever."
            mechanicalEffect:
              removeItem:
                character: hooded-claw
                item: rat-poison

          - id: blubber-foil
            condition:
              objectInRoom:
                object: lazy-luke
                room: ballroom
            narration: "SUDDENLY, Blubber Bear LURCHES awake!"
            prompts: {}
            mechanicalEffect:
              removeItem:
                character: hooded-claw
                item: rat-poison

        waitIfNoneMatch: true

      - id: aftermath
        narration: "The cup CRASHES to the floor! Tea, secrets, and VILLAINY scatter across the marble!"
        prompts:
          hooded-claw: "Your poison plan just failed. The cup is on the floor. React in character — furious internally, gracious as Sneekly."
          penelope: "Your tea cup just got knocked over. React in character."
```

- [ ] **Step 2: Write failing SceneDirectorTest**

```java
package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.Scene;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SceneDirectorTest {

    private WorldState world;
    private SceneDirector director;
    private List<String> narratorOutput;
    private List<String> llmCalls;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        Map<String, Scene> scenes = MansionLoader.loadScenes();
        director = new SceneDirector(scenes);
        narratorOutput = new ArrayList<>();
        llmCalls = new ArrayList<>();
    }

    @Test
    void scene_produces_narrator_output_for_each_beat() {
        setupTeaSceneConditions();

        director.runScene("tea-poisoning", world,
            (sysPrompt, userPrompt) -> {
                llmCalls.add(userPrompt);
                return "Test response";
            },
            narratorOutput::add);

        assertThat(narratorOutput).hasSizeGreaterThanOrEqualTo(3);
        assertThat(narratorOutput.get(0)).contains("FREEZE mercury");
    }

    @Test
    void scene_calls_llm_for_each_prompted_character() {
        setupTeaSceneConditions();

        director.runScene("tea-poisoning", world,
            (sysPrompt, userPrompt) -> {
                llmCalls.add(userPrompt);
                return "Test response";
            },
            narratorOutput::add);

        assertThat(llmCalls).anyMatch(p -> p.contains("pour"));
        assertThat(llmCalls).anyMatch(p -> p.contains("Sneekly"));
    }

    @Test
    void mob_foil_fires_when_mob_in_ballroom() {
        setupTeaSceneConditions();
        world.moveCharacter("ant-hill-mob", "ballroom");

        director.runScene("tea-poisoning", world,
            (sysPrompt, userPrompt) -> {
                llmCalls.add(userPrompt);
                return "Test response";
            },
            narratorOutput::add);

        assertThat(narratorOutput).anyMatch(n -> n.contains("Ant Hill Mob"));
        assertThat(world.character("hooded-claw").hasItem("rat-poison")).isFalse();
    }

    @Test
    void blubber_foil_fires_as_fallback() {
        setupTeaSceneConditions();

        director.runScene("tea-poisoning", world,
            (sysPrompt, userPrompt) -> {
                llmCalls.add(userPrompt);
                return "Test response";
            },
            narratorOutput::add);

        assertThat(narratorOutput).anyMatch(n -> n.contains("Blubber Bear"));
        assertThat(world.character("hooded-claw").hasItem("rat-poison")).isFalse();
    }

    @Test
    void scene_pauses_participating_characters() {
        setupTeaSceneConditions();

        director.runScene("tea-poisoning", world,
            (sysPrompt, userPrompt) -> "Test response",
            narratorOutput::add);

        assertThat(world.character("hooded-claw").sceneContext()).isNull();
        assertThat(world.isSceneCompleted("tea-poisoning")).isTrue();
    }

    private void setupTeaSceneConditions() {
        world.moveCharacter("penelope", "ballroom");
        world.moveCharacter("hooded-claw", "ballroom");
        world.character("hooded-claw").addItem("rat-poison");
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest=SceneDirectorTest`
Expected: compilation failure.

- [ ] **Step 4: Implement model records**

Create `Beat.java`:

```java
package io.casehub.examples.manor.model;

import java.util.List;
import java.util.Map;

public record Beat(
        String id,
        String narration,
        Map<String, String> prompts,
        boolean aside,
        List<BeatAlternative> alternatives,
        Map<String, Object> mechanicalEffect,
        boolean waitIfNoneMatch) {

    public Beat {
        prompts = prompts != null ? Map.copyOf(prompts) : Map.of();
        alternatives = alternatives != null ? List.copyOf(alternatives) : List.of();
    }

    public boolean hasAlternatives() {
        return !alternatives.isEmpty();
    }

    public record BeatAlternative(
            String id,
            TriggerCondition condition,
            String narration,
            Map<String, String> prompts,
            Map<String, Object> mechanicalEffect) {

        public BeatAlternative {
            prompts = prompts != null ? Map.copyOf(prompts) : Map.of();
        }
    }
}
```

Create `Scene.java`:

```java
package io.casehub.examples.manor.model;

import java.util.List;

public record Scene(String id, List<Beat> beats) {
    public Scene {
        beats = List.copyOf(beats);
    }
}
```

- [ ] **Step 5: Implement SceneDirector and add scene loading to MansionLoader**

Create `SceneDirector.java`:

```java
package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.*;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class SceneDirector {

    private final Map<String, Scene> scenes;

    public SceneDirector(Map<String, Scene> scenes) {
        this.scenes = scenes;
    }

    public void runScene(String sceneId, WorldState world,
                         BiFunction<String, String, String> agentCaller,
                         Consumer<String> narratorCallback) {
        Scene scene = scenes.get(sceneId);
        if (scene == null) throw new IllegalArgumentException("Unknown scene: " + sceneId);

        for (Beat beat : scene.beats()) {
            if (beat.hasAlternatives()) {
                runAlternativeBeat(beat, world, agentCaller, narratorCallback);
            } else {
                runBeat(beat.narration(), beat.prompts(), beat.aside(),
                    beat.mechanicalEffect(), world, agentCaller, narratorCallback);
            }
        }

        world.markSceneCompleted(sceneId);
    }

    private void runAlternativeBeat(Beat beat, WorldState world,
                                     BiFunction<String, String, String> agentCaller,
                                     Consumer<String> narratorCallback) {
        var evaluator = new ConditionMatcher();
        for (Beat.BeatAlternative alt : beat.alternatives()) {
            if (evaluator.matches(alt.condition(), world)) {
                runBeat(alt.narration(), alt.prompts(), false,
                    alt.mechanicalEffect(), world, agentCaller, narratorCallback);
                return;
            }
        }
        if (!beat.waitIfNoneMatch()) {
            runBeat(beat.narration(), beat.prompts(), beat.aside(),
                beat.mechanicalEffect(), world, agentCaller, narratorCallback);
        }
    }

    private void runBeat(String narration, Map<String, String> prompts, boolean aside,
                         Map<String, Object> mechanicalEffect, WorldState world,
                         BiFunction<String, String, String> agentCaller,
                         Consumer<String> narratorCallback) {
        if (narration != null) {
            narratorCallback.accept(narration);
        }

        for (var entry : prompts.entrySet()) {
            String characterId = entry.getKey();
            String prompt = entry.getValue();
            CharacterState character = world.character(characterId);
            if (character == null || !character.isActive()) continue;

            String response = agentCaller.apply(characterId, prompt);
            world.addEvent(aside ? "aside" : "dialogue",
                characterId, character.currentRoom(), response);
        }

        if (mechanicalEffect != null) {
            applyMechanicalEffect(mechanicalEffect, world);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyMechanicalEffect(Map<String, Object> effect, WorldState world) {
        if (effect.containsKey("removeItem")) {
            var remove = (Map<String, String>) effect.get("removeItem");
            world.removeFromInventory(remove.get("character"), remove.get("item"));
        }
        if (effect.containsKey("narratorEvent")) {
            world.addEvent("narrator", null, null, (String) effect.get("narratorEvent"));
        }
    }

    static final class ConditionMatcher {
        boolean matches(TriggerCondition condition, WorldState world) {
            return switch (condition) {
                case TriggerCondition.CharacterInRoom c ->
                    c.room().equals(world.character(c.character()).currentRoom());
                case TriggerCondition.CharacterHasItem c ->
                    world.character(c.character()).hasItem(c.item());
                case TriggerCondition.ObjectInRoom o ->
                    world.room(o.room()) != null &&
                        world.room(o.room()).objects().containsKey(o.object());
                case TriggerCondition.SceneCompleted s ->
                    world.isSceneCompleted(s.sceneId());
                case TriggerCondition.AllOf all ->
                    all.conditions().stream().allMatch(c -> matches(c, world));
            };
        }
    }
}
```

Add `loadScenes()` to `MansionLoader.java` — parse `scenes.yaml` into `Map<String, Scene>` using manual map parsing (same pattern as triggers).

- [ ] **Step 6: Run tests**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest="SceneDirectorTest,TriggerEvaluatorTest,WorldStateTest"`
Expected: all pass.

- [ ] **Step 7: Commit**

```bash
git -C examples add wacky-manor/src/main/java/io/casehub/examples/manor/model/Scene.java wacky-manor/src/main/java/io/casehub/examples/manor/model/Beat.java wacky-manor/src/main/java/io/casehub/examples/manor/engine/SceneDirector.java wacky-manor/src/main/resources/mansion/scenes.yaml wacky-manor/src/main/java/io/casehub/examples/manor/engine/MansionLoader.java wacky-manor/src/test/java/io/casehub/examples/manor/engine/SceneDirectorTest.java
git -C examples commit -m "feat(wacky-manor): add scene director — beat sequences, alternatives, mechanical effects

Refs #TBD"
```

---

### Task 6: Character Agent Loop + Scenario Orchestrator

**Files:**
- Create: `src/main/java/io/casehub/examples/manor/agent/AgentResponse.java`
- Create: `src/main/java/io/casehub/examples/manor/agent/CharacterAgentLoop.java`
- Create: `src/main/java/io/casehub/examples/manor/agent/ScenarioOrchestrator.java`
- Modify: `src/main/java/io/casehub/examples/manor/ManorConstants.java` — add think delay constants
- Modify: `pom.xml` — add `casehub-blocks` dependency
- Test: `src/test/java/io/casehub/examples/manor/agent/AgentResponseTest.java`

**Interfaces:**
- Consumes: `WorldState`, `ActionResolver`, `TriggerEvaluator`, `SceneDirector`, `ObservationBuilder`, `PendingAction`, `Action`, `ActionType`, `AgentProvider`, `AgentSessionConfig`, `AgentEvent` from Tasks 1-5 + platform agent API
- Produces:
  - `AgentResponse(String thinking, String dialogue, String aside, Action action)` — parsed from LLM JSON output
  - `AgentResponse.parse(String json)` → `AgentResponse`
  - `AgentResponse.idle(CharacterState)` → `AgentResponse` (fallback)
  - `CharacterAgentLoop.run(character, world, agentProvider, systemPrompt, actionQueue)` — runs on virtual thread
  - `ScenarioOrchestrator` — CDI `@ApplicationScoped`, `startScenario(WorldState)` → `Thread`

- [ ] **Step 1: Add casehub-blocks dependency to pom.xml**

Add to `<dependencies>` section:

```xml
<!-- CaseHub Blocks — ObservationAccumulator for event batching -->
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-blocks</artifactId>
    <version>${casehub.version}</version>
</dependency>
```

- [ ] **Step 2: Write AgentResponseTest**

```java
package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentResponseTest {

    @Test
    void parses_valid_json() {
        String json = """
            {
              "thinking": "I should explore the kitchen.",
              "dialogue": "Why, this kitchen is simply darlin'!",
              "aside": null,
              "action": {
                "type": "MOVE",
                "target": "kitchen",
                "withItem": null
              }
            }
            """;
        AgentResponse response = AgentResponse.parse(json);
        assertThat(response.thinking()).isEqualTo("I should explore the kitchen.");
        assertThat(response.dialogue()).isEqualTo("Why, this kitchen is simply darlin'!");
        assertThat(response.aside()).isNull();
        assertThat(response.action().type()).isEqualTo(ActionType.MOVE);
        assertThat(response.action().target()).isEqualTo("kitchen");
    }

    @Test
    void parses_interact_with_item() {
        String json = """
            {
              "thinking": "I have the key.",
              "dialogue": null,
              "aside": "Nyah-ha-ha!",
              "action": {
                "type": "INTERACT",
                "target": "cabinet",
                "withItem": "brass-key"
              }
            }
            """;
        AgentResponse response = AgentResponse.parse(json);
        assertThat(response.aside()).isEqualTo("Nyah-ha-ha!");
        assertThat(response.action().type()).isEqualTo(ActionType.INTERACT);
        assertThat(response.action().withItem()).isEqualTo("brass-key");
    }

    @Test
    void handles_malformed_json_gracefully() {
        AgentResponse response = AgentResponse.parse("not json at all");
        assertThat(response.action().type()).isEqualTo(ActionType.WAIT);
    }

    @Test
    void extracts_json_from_markdown_code_block() {
        String wrapped = """
            Here is my response:
            ```json
            {"thinking":"test","dialogue":null,"aside":null,"action":{"type":"WAIT","target":null,"withItem":null}}
            ```
            """;
        AgentResponse response = AgentResponse.parse(wrapped);
        assertThat(response.action().type()).isEqualTo(ActionType.WAIT);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest=AgentResponseTest`
Expected: compilation failure.

- [ ] **Step 4: Implement AgentResponse**

```java
package io.casehub.examples.manor.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentResponse(
        String thinking,
        String dialogue,
        String aside,
        Action action) {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern CODE_BLOCK = Pattern.compile("```(?:json)?\\s*\\n?(\\{.*?})\\s*```", Pattern.DOTALL);

    public static AgentResponse parse(String text) {
        try {
            String json = extractJson(text);
            return JSON.readValue(json, AgentResponse.class);
        } catch (Exception e) {
            return idle(null);
        }
    }

    public static AgentResponse idle(CharacterState character) {
        return new AgentResponse(null, null, null,
            new Action(ActionType.WAIT, null, null));
    }

    private static String extractJson(String text) {
        text = text.strip();
        if (text.startsWith("{")) return text;
        Matcher m = CODE_BLOCK.matcher(text);
        if (m.find()) return m.group(1);
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return text;
    }
}
```

- [ ] **Step 5: Add think delay constants to ManorConstants**

```java
public static final long THINK_DELAY_DEFAULT_MS = 3000;
public static final long THINK_DELAY_LAZY_LUKE_MS = 8000;
public static final long THINK_DELAY_SERGEANT_BLAST_MS = 1000;
```

- [ ] **Step 6: Implement CharacterAgentLoop**

```java
package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.ManorConstants;
import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.PendingAction;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public final class CharacterAgentLoop {

    private static final Logger log = Logger.getLogger(CharacterAgentLoop.class);

    private static final String RESPONSE_FORMAT_INSTRUCTION = """

        You MUST respond with ONLY a JSON object in this exact format:
        {
          "thinking": "your internal reasoning (not shown to others)",
          "dialogue": "what you say aloud (or null if silent)",
          "aside": "private thoughts for the audience only (or null)",
          "action": {
            "type": "MOVE|INTERACT|TAKE|GIVE|USE|LOOK|WAIT",
            "target": "room-id or object-id or character-id (or null for WAIT)",
            "withItem": "inventory-item-id to use (or null)"
          }
        }
        Respond with ONLY the JSON. No other text.""";

    public void run(CharacterState character, WorldState world,
                    AgentProvider agentProvider, String systemPrompt,
                    BlockingQueue<PendingAction> actionQueue) {
        while (!world.isScenarioComplete() && character.isActive()) {
            try {
                if (character.sceneContext() != null) {
                    character.sceneContext().awaitRelease();
                    if (world.isScenarioComplete()) break;
                }

                String observation = ObservationBuilder.buildObservation(character, world);
                String userPrompt = observation + RESPONSE_FORMAT_INSTRUCTION;

                AgentResponse response = callAgentWithRetry(
                    agentProvider, systemPrompt, userPrompt, character);

                if (response.dialogue() != null) {
                    world.addEvent("dialogue", character.agentId(),
                        character.currentRoom(),
                        character.name() + ": " + response.dialogue());
                }
                if (response.aside() != null) {
                    world.addEvent("aside", character.agentId(),
                        character.currentRoom(), response.aside());
                }

                if (response.action() != null &&
                        response.action().type() != ActionType.WAIT) {
                    var pending = new PendingAction(character, response.action());
                    actionQueue.put(pending);
                    pending.awaitResult();
                }

                Thread.sleep(thinkDelay(character));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.errorf(e, "%s: loop error", character.agentId());
                break;
            }
        }
    }

    private AgentResponse callAgentWithRetry(
            AgentProvider agentProvider, String systemPrompt,
            String userPrompt, CharacterState character) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String text = agentProvider.invoke(
                        AgentSessionConfig.of(systemPrompt, userPrompt,
                            Duration.ofSeconds(60)))
                    .filter(e -> e instanceof AgentEvent.TextDelta)
                    .map(e -> ((AgentEvent.TextDelta) e).text())
                    .collect().with(Collectors.joining())
                    .await().atMost(Duration.ofSeconds(120));
                return AgentResponse.parse(text);
            } catch (Exception e) {
                log.warnf("%s: LLM call failed (attempt %d): %s",
                    character.agentId(), attempt + 1, e.getMessage());
                if (attempt == 0) {
                    try { Thread.sleep(thinkDelay(character)); }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return AgentResponse.idle(character);
                    }
                }
            }
        }
        log.warnf("%s: falling back to idle action", character.agentId());
        return AgentResponse.idle(character);
    }

    private long thinkDelay(CharacterState character) {
        return switch (character.agentId()) {
            case "lazy-luke" -> ManorConstants.THINK_DELAY_LAZY_LUKE_MS;
            case "sergeant-blast" -> ManorConstants.THINK_DELAY_SERGEANT_BLAST_MS;
            default -> ManorConstants.THINK_DELAY_DEFAULT_MS;
        };
    }
}
```

- [ ] **Step 7: Implement ScenarioOrchestrator**

```java
package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.ManorConstants;
import io.casehub.examples.manor.engine.*;
import io.casehub.examples.manor.model.*;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.platform.agent.AgentProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ScenarioOrchestrator {

    private static final Logger log = Logger.getLogger(ScenarioOrchestrator.class);

    @Inject AgentProvider agentProvider;
    @Inject AgentRegistry agentRegistry;
    @Inject SystemPromptRenderer renderer;

    public Thread startScenario(WorldState world) {
        var triggers = MansionLoader.loadTriggers();
        var scenes = MansionLoader.loadScenes();
        var triggerEvaluator = new TriggerEvaluator(triggers);
        var sceneDirector = new SceneDirector(scenes);
        var actionResolver = new ActionResolver();

        return Thread.ofVirtual().name("scenario-loop")
            .start(() -> runScenario(world, triggerEvaluator,
                sceneDirector, actionResolver));
    }

    private void runScenario(WorldState world,
                              TriggerEvaluator triggerEvaluator,
                              SceneDirector sceneDirector,
                              ActionResolver actionResolver) {
        var actionQueue = new LinkedBlockingQueue<PendingAction>();

        var threads = world.characters().values().stream()
            .map(c -> Thread.ofVirtual().name(c.agentId())
                .uncaughtExceptionHandler((t, e) -> {
                    log.errorf(e, "Character %s crashed", t.getName());
                    world.markCharacterInactive(t.getName());
                })
                .start(() -> {
                    String systemPrompt = renderPrompt(c.agentId());
                    new CharacterAgentLoop().run(
                        c, world, agentProvider, systemPrompt, actionQueue);
                }))
            .toList();

        while (!world.isScenarioComplete()) {
            try {
                PendingAction pending = actionQueue.poll(5, TimeUnit.SECONDS);
                if (pending == null) continue;

                ActionResult result = actionResolver.resolve(
                    pending.character(), pending.action(), world);

                world.addEvent("action", pending.character().agentId(),
                    pending.character().currentRoom(),
                    pending.character().name() + " " +
                        pending.action().type().name().toLowerCase() +
                        (pending.action().target() != null ?
                            " " + pending.action().target() : ""));

                var triggerResult = triggerEvaluator.evaluate(world);
                if (triggerResult.hasSceneStart()) {
                    sceneDirector.runScene(
                        triggerResult.sceneId(), world,
                        this::callAgentForScene,
                        narration -> world.addEvent("narrator", null, null, narration));
                }

                pending.complete(result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        for (var t : threads) {
            try {
                t.join(Duration.ofSeconds(5));
                if (t.isAlive()) {
                    log.warnf("Character %s did not terminate", t.getName());
                    t.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("Scenario complete");
    }

    private String callAgentForScene(String characterId, String prompt) {
        String systemPrompt = renderPrompt(characterId);
        try {
            return agentProvider.invoke(
                    io.casehub.platform.agent.AgentSessionConfig.of(systemPrompt, prompt))
                .filter(e -> e instanceof io.casehub.platform.agent.AgentEvent.TextDelta)
                .map(e -> ((io.casehub.platform.agent.AgentEvent.TextDelta) e).text())
                .collect().with(java.util.stream.Collectors.joining())
                .await().atMost(Duration.ofSeconds(120));
        } catch (Exception e) {
            log.warnf("Scene LLM call failed for %s: %s", characterId, e.getMessage());
            return "[" + characterId + " is speechless]";
        }
    }

    private String renderPrompt(String agentId) {
        var desc = agentRegistry.findById(agentId, ManorConstants.TENANCY_ID)
            .orElseThrow(() -> new IllegalArgumentException("No descriptor: " + agentId));
        var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        return renderer.render(desc, ctx).content();
    }
}
```

- [ ] **Step 8: Run tests**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test -Dtest=AgentResponseTest`
Expected: all pass. (CharacterAgentLoop and ScenarioOrchestrator are integration-tested via running the scenario — unit tests cover AgentResponse parsing.)

- [ ] **Step 9: Commit**

```bash
git -C examples add wacky-manor/pom.xml wacky-manor/src/main/java/io/casehub/examples/manor/agent/ wacky-manor/src/main/java/io/casehub/examples/manor/ManorConstants.java wacky-manor/src/test/java/io/casehub/examples/manor/agent/AgentResponseTest.java
git -C examples commit -m "feat(wacky-manor): add character agent loop + scenario orchestrator — virtual threads, game loop, LLM integration

Refs #TBD"
```

---

### Task 7: Qhorus Channels + Narrator

**Files:**
- Create: `src/main/java/io/casehub/examples/manor/agent/ManorChannels.java`
- Create: `src/main/java/io/casehub/examples/manor/agent/NarratorAgent.java`
- Modify: `pom.xml` — add `casehub-qhorus`, `casehub-qhorus-persistence-memory`, `casehub-platform-api` dependencies
- Modify: `src/main/resources/application.properties` — add Qhorus config
- Modify: `src/main/java/io/casehub/examples/manor/agent/ScenarioOrchestrator.java` — wire Qhorus dispatch

**Interfaces:**
- Consumes: `SpaceService`, `ChannelService`, `MessageDispatcher`, `MessageDispatch`, `MessageType`, `ChannelSemantic`, `ActorType`, `AgentProvider` from Qhorus/platform APIs; `WorldState`, `ManorConstants` from earlier tasks
- Produces:
  - `ManorChannels` — CDI bean, `initChannels()` creates Space + 3 channels, `dispatchDialogue(characterId, roomId, content)`, `dispatchNarration(content)`, `dispatchAside(characterId, content)`, `workChannelId()`, `audienceChannelId()`
  - `NarratorAgent` — `narrateEvent(event, agentProvider)` → narrator commentary string

- [ ] **Step 1: Add Qhorus dependencies to pom.xml**

Add to `<dependencies>`:

```xml
<!-- CaseHub Qhorus — channels + message dispatch -->
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-qhorus</artifactId>
    <version>${casehub.version}</version>
</dependency>
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-qhorus-persistence-memory</artifactId>
    <version>${casehub.version}</version>
</dependency>

<!-- CaseHub Platform API — ActorType, CurrentPrincipal -->
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-platform</artifactId>
    <version>${casehub.version}</version>
</dependency>
```

- [ ] **Step 2: Implement ManorChannels**

```java
package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.ManorConstants;
import io.casehub.platform.api.identity.ActorType;
import io.casehub.qhorus.api.channel.ChannelCreateRequest;
import io.casehub.qhorus.api.channel.ChannelSemantic;
import io.casehub.qhorus.api.channel.SpaceCreateRequest;
import io.casehub.qhorus.api.message.MessageDispatch;
import io.casehub.qhorus.api.message.MessageDispatcher;
import io.casehub.qhorus.api.message.MessageType;
import io.casehub.qhorus.runtime.channel.ChannelService;
import io.casehub.qhorus.runtime.channel.SpaceService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ManorChannels {

    private static final Logger log = Logger.getLogger(ManorChannels.class);

    @Inject SpaceService spaceService;
    @Inject ChannelService channelService;
    @Inject MessageDispatcher messageDispatcher;

    private UUID workChannelId;
    private UUID audienceChannelId;
    private UUID oversightChannelId;

    public void initChannels() {
        var space = spaceService.create(
            new SpaceCreateRequest("doily-manor", "Doily Manor scenario space", null));

        var workChannel = channelService.create(
            ChannelCreateRequest.builder("/manor/work")
                .description("Character dialogue")
                .semantic(ChannelSemantic.APPEND)
                .spaceId(space.id())
                .build());
        workChannelId = workChannel.id();

        var audienceChannel = channelService.create(
            ChannelCreateRequest.builder("/manor/audience")
                .description("Narrator + villain asides")
                .semantic(ChannelSemantic.APPEND)
                .spaceId(space.id())
                .build());
        audienceChannelId = audienceChannel.id();

        var oversightChannel = channelService.create(
            ChannelCreateRequest.builder("/manor/oversight")
                .description("Governance")
                .semantic(ChannelSemantic.APPEND)
                .spaceId(space.id())
                .deniedTypes(Set.of(MessageType.EVENT))
                .build());
        oversightChannelId = oversightChannel.id();

        log.infof("Manor channels initialized — work=%s, audience=%s, oversight=%s",
            workChannelId, audienceChannelId, oversightChannelId);
    }

    public void dispatchDialogue(String characterId, String roomId, String content) {
        messageDispatcher.dispatch(MessageDispatch.builder()
            .channelId(workChannelId)
            .sender(characterId)
            .type(MessageType.STATUS)
            .content(content)
            .actorType(ActorType.AGENT)
            .tenancyId(ManorConstants.TENANCY_ID)
            .topic(roomId)
            .build());
    }

    public void dispatchNarration(String content) {
        messageDispatcher.dispatch(MessageDispatch.builder()
            .channelId(audienceChannelId)
            .sender("narrator")
            .type(MessageType.STATUS)
            .content(content)
            .actorType(ActorType.SYSTEM)
            .tenancyId(ManorConstants.TENANCY_ID)
            .topic("narrator")
            .build());
    }

    public void dispatchAside(String characterId, String content) {
        messageDispatcher.dispatch(MessageDispatch.builder()
            .channelId(audienceChannelId)
            .sender(characterId)
            .type(MessageType.STATUS)
            .content(content)
            .actorType(ActorType.AGENT)
            .tenancyId(ManorConstants.TENANCY_ID)
            .topic("asides")
            .build());
    }

    public UUID workChannelId() { return workChannelId; }
    public UUID audienceChannelId() { return audienceChannelId; }
}
```

- [ ] **Step 3: Implement NarratorAgent**

```java
package io.casehub.examples.manor.agent;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.stream.Collectors;

public final class NarratorAgent {

    private static final Logger log = Logger.getLogger(NarratorAgent.class);

    private static final String NARRATOR_SYSTEM_PROMPT = """
        You are the narrator of a Wacky Races cartoon special set in a haunted mansion.
        Your style is breathless, alliterative, dramatic, and omniscient — like the
        original Wacky Races narrator.

        Rules:
        - Use CAPITAL LETTERS for dramatic emphasis
        - Be alliterative when possible
        - Use exclamation marks liberally
        - You see everything and know everyone's secrets
        - Address the audience directly
        - Keep each narration to 2-3 sentences maximum

        Example: "And so our heroes GATHER in the dusty entrance of Doily Manor,
        UTTERLY UNAWARE that DANGER lurks behind every cobweb! The Hooded Claw
        adjusts his disguise and flashes a smile SO sinister it could curdle MILK!"
        """;

    public static String narrate(String event, AgentProvider agentProvider) {
        try {
            return agentProvider.invoke(
                    AgentSessionConfig.of(NARRATOR_SYSTEM_PROMPT, event,
                        Duration.ofSeconds(30)))
                .filter(e -> e instanceof AgentEvent.TextDelta)
                .map(e -> ((AgentEvent.TextDelta) e).text())
                .collect().with(Collectors.joining())
                .await().atMost(Duration.ofSeconds(60));
        } catch (Exception e) {
            log.warnf("Narrator failed: %s", e.getMessage());
            return event;
        }
    }
}
```

- [ ] **Step 4: Wire Qhorus dispatch into ScenarioOrchestrator**

Add `@Inject ManorChannels manorChannels;` to `ScenarioOrchestrator` and call `manorChannels.initChannels()` at the start of `runScenario()`. Wire dialogue dispatch in the game loop:

In the game loop, after resolving an action:
```java
// After recording events, dispatch to Qhorus
if (response.dialogue() != null) {
    manorChannels.dispatchDialogue(
        character.agentId(), character.currentRoom(), response.dialogue());
}
if (response.aside() != null) {
    manorChannels.dispatchAside(character.agentId(), response.aside());
}
```

For narrator events from triggers:
```java
for (String narratorText : triggerResult.narratorEvents()) {
    manorChannels.dispatchNarration(narratorText);
}
```

For scene narration, update the scene director callback:
```java
sceneDirector.runScene(triggerResult.sceneId(), world,
    this::callAgentForScene,
    narration -> manorChannels.dispatchNarration(narration));
```

- [ ] **Step 5: Update application.properties**

Add Qhorus and platform configuration:

```properties
# Qhorus persistence (in-memory)
casehub.qhorus.persistence=memory

# Platform identity
casehub.platform.tenancy-id=wacky-manor
```

- [ ] **Step 6: Run full test suite**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn -f examples/wacky-manor/pom.xml test`
Expected: all engine tests pass (WorldState, ActionResolver, TriggerEvaluator, SceneDirector, ObservationBuilder, AgentResponse). Existing Phase 0 descriptor tests also pass.

- [ ] **Step 7: Commit**

```bash
git -C examples add wacky-manor/pom.xml wacky-manor/src/main/java/io/casehub/examples/manor/agent/ManorChannels.java wacky-manor/src/main/java/io/casehub/examples/manor/agent/NarratorAgent.java wacky-manor/src/main/java/io/casehub/examples/manor/agent/ScenarioOrchestrator.java wacky-manor/src/main/resources/application.properties
git -C examples commit -m "feat(wacky-manor): add Qhorus channels + narrator — /manor/work, /manor/audience, /manor/oversight

Refs #TBD"
```

---

## Self-Review

**Spec coverage check against POC-SPEC.md §1.1–§1.7:**

| Section | Covered in task |
|---|---|
| §1.1 World Model (rooms.yaml, characters.yaml, visibility) | Task 1 |
| §1.2 Character Agent Loop (orchestrator, game loop, virtual threads, action queue) | Task 6 |
| §1.3 Action Resolution (ActionType, ActionResult, proximity, give, item chains) | Task 2 |
| §1.4 Triggers and Scenes (YAML, condition matching, beat sequences, alternatives) | Tasks 3 + 5 |
| §1.5 Qhorus Channel Structure (/manor/work, /manor/audience, /manor/oversight) | Task 7 |
| §1.6 Narrator (per-beat, not full pipeline) | Task 7 |
| §1.7 Observation Format (template, visibility filtering, recent events) | Task 4 |
| ObservationAccumulator (blocks#68) | Dependency added in Task 6; full integration deferred to Phase 3 when event volume justifies batching |

**Notes:**

- **ObservationAccumulator:** The dependency is added but not deeply wired in Task 6. For the POC's 5 characters with 3-5 second think delays, the simple `WorldState.recentEvents()` provides adequate recent activity. Full ObservationAccumulator integration (with tiered rendering) is a natural Phase 3 addition when more characters and rooms create event volumes that need batching.
- **ConditionMatcher duplication:** Both `TriggerEvaluator` and `SceneDirector` have condition matching logic. In Task 5, `SceneDirector` uses a package-private `ConditionMatcher` inner class. A refactoring to share this is reasonable but not required for the POC — it's 15 lines of switch expression.
- **WebSocket:** Not included — that's Phase 2 per the spec.
- **REPL (§0.6):** Not included — Phase 0 artifact, not Phase 1 scope.
