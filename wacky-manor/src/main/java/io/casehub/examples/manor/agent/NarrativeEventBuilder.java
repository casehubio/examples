package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionResult;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;

public final class NarrativeEventBuilder {

    public static String describe(CharacterState character, Action action, ActionResult result) {
        if (result instanceof ActionResult.Failed) return null;
        if (action.type() == ActionType.WAIT) return null;

        String name = character.name();
        return switch (action.type()) {
            case MOVE -> name + " walked into the " + action.target() + ".";
            case TAKE -> name + " picked up something.";
            case USE -> name + " fussed with the " + action.target() + " for a moment.";
            case GIVE -> name + " handed something to " + action.target() + ".";
            case LOOK -> name + " examined the " + action.target() + ".";
            case INTERACT -> name + " interacted with the " + action.target() + ".";
            case WAIT -> null;
        };
    }
}
