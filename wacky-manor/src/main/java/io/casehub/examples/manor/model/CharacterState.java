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
