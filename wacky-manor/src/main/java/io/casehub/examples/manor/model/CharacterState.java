package io.casehub.examples.manor.model;

public final class CharacterState {
    private final    String                                            agentId;
    private final    String                                            name;
    private volatile String                                            currentRoom;
    private          double                                            x;
    private final    java.util.concurrent.CopyOnWriteArrayList<String> inventory;
    private volatile boolean                                           active           = true;
    private volatile SceneContext                                      sceneContext;
    private          String                                            lastActionResult = "You have just arrived at Doily Manor.";
    private final    long                                              thinkDelayMs;
    private volatile java.util.Set<String>                             capabilityTags   = java.util.Set.of();
    private final    java.util.concurrent.ConcurrentHashMap<String, AgentPlan> plans = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile String                                                    currentThinking;

    public CharacterState(String agentId, String name, String startRoom,
                          double startX, java.util.List<String> inventory) {
        this(agentId, name, startRoom, startX, inventory,
             io.casehub.examples.manor.ManorConstants.THINK_DELAY_DEFAULT_MS);
    }

    public CharacterState(String agentId, String name, String startRoom,
                          double startX, java.util.List<String> inventory, long thinkDelayMs) {
        this.agentId      = agentId;
        this.name         = name;
        this.currentRoom  = startRoom;
        this.x            = startX;
        this.inventory    = new java.util.concurrent.CopyOnWriteArrayList<>(
                inventory != null ? inventory : java.util.List.of());
        this.thinkDelayMs = thinkDelayMs;
    }

    public String agentId()                                   {return agentId;}

    public String name()                                      {return name;}

    public String currentRoom()                               {return currentRoom;}

    public double x()                                         {return x;}

    public java.util.List<String> inventory()                 {return java.util.List.copyOf(inventory);}

    public boolean isActive()                                 {return active;}

    public long thinkDelayMs()                                {return thinkDelayMs;}

    public SceneContext sceneContext()                        {return sceneContext;}

    public String lastActionResult()                          {return lastActionResult;}

    public java.util.Set<String> capabilityTags()             {return java.util.Set.copyOf(capabilityTags);}

    public void setCapabilityTags(java.util.Set<String> tags) {this.capabilityTags = java.util.Set.copyOf(tags);}

    public java.util.Map<String, AgentPlan> plans()      {return java.util.Collections.unmodifiableMap(plans);}

    public void setPlan(String goalName, AgentPlan plan) {plans.put(goalName, plan);}

    public void removePlan(String goalName)              {plans.remove(goalName);}

    public String currentThinking()                      {return currentThinking;}

    public void setCurrentThinking(String thinking)      {this.currentThinking = thinking;}

    public void setLastActionResult(String result)            {this.lastActionResult = result;}

    public void setCurrentRoom(String room)                   {this.currentRoom = room;}

    public void setX(double x)                                {this.x = x;}

    public void setActive(boolean active)                     {this.active = active;}

    public void setSceneContext(SceneContext ctx)             {this.sceneContext = ctx;}

    public void addItem(String itemId)                        {inventory.add(itemId);}

    public boolean removeItem(String itemId)                  {return inventory.remove(itemId);}

    public boolean hasItem(String itemId)                     {return inventory.contains(itemId);}
}
