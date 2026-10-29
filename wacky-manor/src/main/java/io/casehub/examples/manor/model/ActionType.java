package io.casehub.examples.manor.model;

public enum ActionType {
    MOVE, INTERACT, TAKE, GIVE, USE, LOOK, WAIT, STEAL, PULL_ASIDE;

    @com.fasterxml.jackson.annotation.JsonCreator
    public static ActionType fromString(String value) {
        if ("EXAMINE".equalsIgnoreCase(value)) return INTERACT;
        return valueOf(value);
    }
}
