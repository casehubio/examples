package io.casehub.examples.manor.model;

import java.util.List;

public final class CharacterState {
    private final String agentId;
    private final String name;
    private volatile String currentRoom;
    private double x;
    private final java.util.concurrent.CopyOnWriteArrayList<String> inventory;
    private volatile boolean active = true;
    private volatile SceneContext sceneContext;
    private          String       lastActionResult = "You have just arrived at Doily Manor.";
    private final    long         thinkDelayMs;
    private volatile java.util.Set<String> capabilityTags = java.util.Set.of();
    private volatile String                currentPlan;
    private static final int               MAX_DYNAMIC_GOALS                          = 5;
    private final java.util.concurrent.CopyOnWriteArrayList<DynamicGoal> dynamicGoals = new java.util.concurrent.CopyOnWriteArrayList<>();


    public CharacterState(String agentId, String name, String startRoom,
                          double startX, List<String> inventory) {
        this(agentId, name, startRoom, startX, inventory,
             io.casehub.examples.manor.ManorConstants.THINK_DELAY_DEFAULT_MS);}

    public CharacterState(String agentId, String name, String startRoom,
                          double startX, List<String> inventory, long thinkDelayMs) {
        this.agentId      = agentId;
        this.name         = name;
        this.currentRoom  = startRoom;
        this.x            = startX;
        this.inventory    = new java.util.concurrent.CopyOnWriteArrayList<>(
                inventory != null ? inventory : List.of());
        this.thinkDelayMs = thinkDelayMs;
    }


    public String agentId() { return agentId; }
    public String name() { return name; }
    public String currentRoom() { return currentRoom; }
    public double x() { return x; }
    public List<String> inventory() { return List.copyOf(inventory); }
    public boolean isActive() { return active; }

    public long thinkDelayMs() {return thinkDelayMs;}

    public SceneContext sceneContext() { return sceneContext; }

    public String lastActionResult()   {return lastActionResult;}

    public java.util.Set<String> capabilityTags() {return java.util.Set.copyOf(capabilityTags);}

    public void setCapabilityTags(java.util.Set<String> tags) {this.capabilityTags = java.util.Set.copyOf(tags);}

    public String currentPlan()                               {return currentPlan;}

    public void setCurrentPlan(String plan)                   {this.currentPlan = plan;}

    public java.util.List<DynamicGoal> dynamicGoals()         {return java.util.List.copyOf(dynamicGoals);}

    public void addDynamicGoal(DynamicGoal goal) {
        dynamicGoals.removeIf(g -> g.name().equals(goal.name()));
        dynamicGoals.add(goal);
        while (dynamicGoals.size() > MAX_DYNAMIC_GOALS) {
            dynamicGoals.remove(0);
        }
    }

    public void dropDynamicGoal(String name) {
        String normalized = name.strip();
        dynamicGoals.removeIf(g -> g.name().equalsIgnoreCase(normalized));
    }

    public void dropAllDynamicGoals() {dynamicGoals.clear();}


    public void setLastActionResult(String result) {this.lastActionResult = result;}


    public void setCurrentRoom(String room) { this.currentRoom = room; }
    public void setX(double x) { this.x = x; }
    public void setActive(boolean active) { this.active = active; }
    public void setSceneContext(SceneContext ctx) { this.sceneContext = ctx; }

    public void addItem(String itemId) { inventory.add(itemId); }
    public boolean removeItem(String itemId) { return inventory.remove(itemId); }
    public boolean hasItem(String itemId) { return inventory.contains(itemId); }
}
