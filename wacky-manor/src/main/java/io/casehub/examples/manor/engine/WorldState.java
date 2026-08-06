package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.GameObject;
import io.casehub.examples.manor.model.ManorEvent;
import io.casehub.examples.manor.model.Room;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WorldState {

    private final Map<String, Room> rooms;
    private final Map<String, CharacterState> characters;
    private final Set<String> firedTriggers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> visibilityOverrides = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<ManorEvent> eventLog = java.util.Collections.synchronizedList(new ArrayList<>());
    private final Set<String> completedScenes = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile boolean scenarioComplete = false;
    private final Set<String> takenObjects = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> appliedEffects = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile io.casehub.examples.manor.model.CompletionReason completionReason;
    private volatile boolean                                          paused          = false;
    private volatile double                                           speedMultiplier = 1.0;


    public WorldState(Map<String, Room> rooms, Map<String, CharacterState> characters) {
        this.rooms = rooms;
        this.characters = characters;
    }

    public Map<String, Room> rooms() { return rooms; }
    public Map<String, CharacterState> characters() {return java.util.Collections.unmodifiableMap(characters);}
    public Room room(String id) { return rooms.get(id); }
    public CharacterState character(String id) { return characters.get(id); }

    public List<GameObject> visibleObjects(String roomId, String characterId) {
        return room(roomId).objects().values().stream()
                           .filter(obj -> !takenObjects.contains(obj.id()))
                           .filter(obj -> isObjectVisible(obj.id(), obj, characterId))
                           .toList();
    }

    private boolean isObjectVisible(String objectId, GameObject obj, String characterId) {
        if (obj.isVisibleToAll()) return true;
        if (obj.visibleTo().contains(characterId)) return true;
        Set<String> overrides = visibilityOverrides.get(objectId);
        return overrides != null && overrides.contains(characterId);
    }

    public void revealObject(String objectId, String characterId) {visibilityOverrides.computeIfAbsent(objectId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(characterId);}

    public void revealObjectToAll(String objectId) {
        for (String charId : characters.keySet()) {
            revealObject(objectId, charId);
        }
    }

    public void moveCharacter(String characterId, String roomId) {
        CharacterState c = characters.get(characterId);
        c.setCurrentRoom(roomId);
        c.setX(0.5);
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

    public void setScenarioComplete(io.casehub.examples.manor.model.CompletionReason reason) {
        this.completionReason = reason;
        this.scenarioComplete = true;
    }

    public void markObjectTaken(String objectId) {
        takenObjects.add(objectId);
    }

    public boolean tryTakeObject(String objectId) {
        return takenObjects.add(objectId);
    }


    public boolean isObjectTaken(String objectId) {
        return takenObjects.contains(objectId);
    }

    public void applyEffect(String objectId, String itemId) {appliedEffects.computeIfAbsent(objectId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(itemId);}

    public boolean hasEffect(String objectId, String itemId) {
        return appliedEffects.getOrDefault(objectId, Set.of()).contains(itemId);
    }

    public io.casehub.examples.manor.model.CompletionReason completionReason() {return completionReason;}

    public boolean isPaused()                                                  {return paused;}

    public void setPaused(boolean paused)                                      {this.paused = paused;}

    public double speedMultiplier()                                            {return speedMultiplier;}

    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = Math.max(0.25, Math.min(8.0, multiplier));
    }


    public boolean hasTriggerFired(String triggerId) { return firedTriggers.contains(triggerId); }
    public void markTriggerFired(String triggerId) { firedTriggers.add(triggerId); }

    public boolean isSceneCompleted(String sceneId) { return completedScenes.contains(sceneId); }
    public void markSceneCompleted(String sceneId) { completedScenes.add(sceneId); }

    public void addEvent(String type, String characterId, String room, String description) {
        eventLog.add(new ManorEvent(Instant.now(), type, characterId, room, description));
    }

    public void addEvent(ManorEvent event) {
        eventLog.add(event);
    }


    public List<ManorEvent> recentEvents(String roomId, int limit) {
        List<ManorEvent> snapshot;
        synchronized (eventLog) {snapshot = new ArrayList<>(eventLog);}
        return snapshot.reversed().stream()
                       .filter(e -> roomId.equals(e.room()))
                       .limit(limit)
                       .toList();
    }

    public List<ManorEvent> allEvents() {
        return List.copyOf(eventLog);
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
