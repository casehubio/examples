package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionResult;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;

public final class NarrativeEventBuilder {

    public static String describe(CharacterState character, Action action, ActionResult result) {
        NarrativeDescription rich = describeRich(character, action, result);
        return rich != null ? rich.publicText() : null;
    }

    public static NarrativeDescription describeRich(CharacterState character, Action action, ActionResult result) {
        if (result instanceof ActionResult.Failed) {return null;}
        if (action.type() == ActionType.WAIT) {return null;}

        String name = character.name();
        String publicText = switch (action.type()) {
            case MOVE -> name + " walked into the " + action.target() + ".";
            case TAKE -> name + " picked up something.";
            case USE -> name + " fussed with the " + action.target() + " for a moment.";
            case GIVE -> name + " handed something to " + action.target() + ".";
            case LOOK -> name + " examined the " + action.target() + ".";
            case INTERACT -> name + " interacted with the " + action.target() + ".";
            case STEAL -> name + " did something near " + action.target() + ".";
            case PULL_ASIDE, WAIT -> null;
        };

        String detailedText = switch (action.type()) {
            case TAKE -> name + " picked up the " + action.target() + ".";
            case USE -> name + " applied " + action.withItem() + " to the " + action.target() + ".";
            case GIVE -> name + " handed " + action.withItem() + " to " + action.target() + ".";
            case STEAL -> name + " slipped " + action.withItem() + " out of " + action.target() + "'s pocket.";
            default -> null;
        };

        return new NarrativeDescription(publicText, detailedText);}

    public static NarrativeDescription describeDirectedDialogue(String speakerName, String targetId, String dialogue) {
        String publicText   = speakerName + " spoke quietly with " + targetId + ".";
        String detailedText = speakerName + ", speaking to " + targetId + ": '" + dialogue + "'";
        return new NarrativeDescription(publicText, detailedText);
    }
}
