package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.GameObject;
import io.casehub.examples.manor.model.ManorEvent;
import io.casehub.examples.manor.model.Room;

import java.time.Instant;
import java.util.*;

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
