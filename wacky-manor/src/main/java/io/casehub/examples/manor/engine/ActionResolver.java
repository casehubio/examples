package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.*;

public final class ActionResolver {

    private static final double PROXIMITY_THRESHOLD = 0.15;

    public ActionResult resolve(CharacterState character, Action action, WorldState world) {
        return switch (action.type()) {
            case MOVE -> resolveMove(character, action, world);
            case INTERACT -> resolveInteract(character, action, world);
            case TAKE -> resolveTake(character, action, world);
            case GIVE -> resolveGive(character, action, world);
            case USE -> resolveUse(character, action, world);
            case LOOK -> resolveLook(character, action, world);
            case WAIT -> new ActionResult.Success("You wait and observe.");
        };
    }

    private ActionResult resolveMove(CharacterState character, Action action, WorldState world) {
        Room currentRoom = world.room(character.currentRoom());
        if (!currentRoom.adjacentRooms().contains(action.target())) {
            return new ActionResult.Failed(
                "Cannot move to " + action.target() + " — not adjacent to " + character.currentRoom());
        }
        Room targetRoom = world.room(action.target());
        if (targetRoom == null) {
            return new ActionResult.Failed("Unknown room: " + action.target());
        }
        world.moveCharacter(character.agentId(), action.target());
        return new ActionResult.MovedToRoom(action.target(),
            "You moved to " + targetRoom.name() + ".");
    }

    private ActionResult resolveInteract(CharacterState character, Action action, WorldState world) {
        Room room = world.room(character.currentRoom());
        GameObject obj = room.objects().get(action.target());
        if (obj == null) {
            return new ActionResult.Failed("No such object: " + action.target());
        }
        if (!obj.interactable()) {
            return new ActionResult.Failed(obj.name() + " cannot be interacted with.");
        }
        if (!isWithinProximity(character.x(), obj.x())) {
            return new ActionResult.Failed("You are too far from " + obj.name() + ".");
        }
        if (obj.interactionRequires() != null) {
            if (action.withItem() == null || !action.withItem().equals(obj.interactionRequires())) {
                return new ActionResult.Failed(obj.name() + " requires " + obj.interactionRequires() + ".");
            }
            character.removeItem(action.withItem());
        }
        if (obj.yields() != null) {
            character.addItem(obj.yields());
            return new ActionResult.ItemReceived(obj.yields(),
                "You received " + obj.yields() + " from " + obj.name() + ".");
        }
        return new ActionResult.Success("You interacted with " + obj.name() + ".");
    }

    private ActionResult resolveTake(CharacterState character, Action action, WorldState world) {
        Room room = world.room(character.currentRoom());
        GameObject obj = room.objects().get(action.target());
        if (obj == null) {
            return new ActionResult.Failed("No such object: " + action.target());
        }
        if (!obj.portable()) {
            return new ActionResult.Failed(obj.name() + " cannot be picked up.");
        }
        if (!isWithinProximity(character.x(), obj.x())) {
            return new ActionResult.Failed("You are too far from " + obj.name() + ".");
        }
        String itemId = "poison".equals(action.target()) ? "rat-poison" : action.target();
        character.addItem(itemId);
        return new ActionResult.ItemReceived(itemId, "You picked up " + obj.name() + ".");
    }

    private ActionResult resolveGive(CharacterState character, Action action, WorldState world) {
        if (action.withItem() == null || !character.hasItem(action.withItem())) {
            return new ActionResult.Failed("You don't have " + action.withItem() + ".");
        }
        CharacterState target = world.character(action.target());
        if (target == null) {
            return new ActionResult.Failed("Unknown character: " + action.target());
        }
        if (!target.currentRoom().equals(character.currentRoom())) {
            return new ActionResult.Failed(target.name() + " is not in this room.");
        }
        if (!isWithinProximity(character.x(), target.x())) {
            return new ActionResult.Failed("You are too far from " + target.name() + ".");
        }
        character.removeItem(action.withItem());
        target.addItem(action.withItem());
        return new ActionResult.Success(
            "You gave " + action.withItem() + " to " + target.name() + ".");
    }

    private ActionResult resolveUse(CharacterState character, Action action, WorldState world) {
        if (action.withItem() == null || !character.hasItem(action.withItem())) {
            return new ActionResult.Failed("You don't have " + action.withItem() + ".");
        }
        Room room = world.room(character.currentRoom());
        GameObject obj = room.objects().get(action.target());
        if (obj == null) {
            return new ActionResult.Failed("No such object: " + action.target());
        }
        if (!isWithinProximity(character.x(), obj.x())) {
            return new ActionResult.Failed("You are too far from " + obj.name() + ".");
        }
        if (!obj.usableWith().contains(action.withItem())) {
            return new ActionResult.Failed(
                action.withItem() + " cannot be used on " + obj.name() + ".");
        }
        return new ActionResult.Success(
            "You used " + action.withItem() + " on " + obj.name() + ".");
    }

    private ActionResult resolveLook(CharacterState character, Action action, WorldState world) {
        if (action.target() == null) {
            Room room = world.room(character.currentRoom());
            return new ActionResult.Success("You look around " + room.name() + ".");
        }
        Room room = world.room(character.currentRoom());
        GameObject obj = room.objects().get(action.target());
        if (obj != null) {
            return new ActionResult.Success("You examine " + obj.name() + ": " + obj.description());
        }
        return new ActionResult.Success("You look at " + action.target() + ".");
    }

    private boolean isWithinProximity(double characterX, double objectX) {
        return Math.abs(characterX - objectX) <= PROXIMITY_THRESHOLD;
    }
}
