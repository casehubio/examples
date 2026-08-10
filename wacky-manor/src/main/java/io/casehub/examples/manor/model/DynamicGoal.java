package io.casehub.examples.manor.model;

public record DynamicGoal(String name, String description, int creationTick) {
    public DynamicGoal {
        name = name.strip().toLowerCase();
    }
}
