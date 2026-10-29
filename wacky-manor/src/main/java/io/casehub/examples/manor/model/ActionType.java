package io.casehub.examples.manor.model;

public enum ActionType {
    MOVE("moved to"),
    INTERACT("examined"),
    TAKE("took"),
    GIVE("gave"),
    USE("used"),
    LOOK("looked at"),
    WAIT("waited"),
    STEAL("took from"),
    PULL_ASIDE("spoke privately with");

    private final String verb;

    ActionType(String verb) { this.verb = verb; }

    public String verb() { return verb; }

    @com.fasterxml.jackson.annotation.JsonCreator
    public static ActionType fromString(String value) {
        if ("EXAMINE".equalsIgnoreCase(value)) return INTERACT;
        return valueOf(value);
    }
}
