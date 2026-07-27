package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.GameObject;
import io.casehub.examples.manor.model.ManorEvent;
import io.casehub.examples.manor.model.Room;

import java.util.List;

public final class ObservationBuilder {

    private static final int RECENT_EVENT_LIMIT = 5;

    public static String buildObservation(CharacterState character, WorldState world) {
        var  sb   = new StringBuilder();
        Room room = world.room(character.currentRoom());

        appendLocation(sb, room);
        appendAdjacentRooms(sb, room, world);
        appendVisibleObjects(sb, character, world);
        appendCharactersPresent(sb, character, world);
        appendInventory(sb, character);
        appendRecentActivity(sb, character, world);

        return sb.toString();}

    private static void appendLocation(StringBuilder sb, Room room) {
        sb.append("== Current Location ==\n");
        sb.append(room.name()).append(": ").append(room.description()).append("\n\n");
    }

    private static void appendAdjacentRooms(StringBuilder sb, Room room, WorldState world) {
        sb.append("== Exits ==\n");
        sb.append("You can MOVE to: ");
        var names = room.adjacentRooms().stream()
                        .map(id -> world.room(id).name() + " (" + id + ")")
                        .toList();
        sb.append(String.join(", ", names));
        sb.append("\nExplore other rooms to discover objects and advance the story.\n\n");
    }


    private static void appendVisibleObjects(StringBuilder sb, CharacterState character,
                                              WorldState world) {
        sb.append("== Visible Objects ==\n");
        List<GameObject> objects = world.visibleObjects(character.currentRoom(), character.agentId());
        if (objects.isEmpty()) {
            sb.append("Nothing notable here.\n");
        } else {
            for (GameObject obj : objects) {
                sb.append("- ").append(obj.name())
                  .append(" (at position ").append(obj.x()).append("): ")
                  .append(obj.description());
                if (obj.interactable()) {
                    sb.append(" [interactable");
                    if (obj.interactionRequires() != null) {
                        sb.append(", requires: ").append(obj.interactionRequires());
                    }
                    sb.append("]");
                }
                if (obj.portable()) {
                    sb.append(" [can be picked up]");
                }
                sb.append("\n");
            }
        }
        sb.append("\n");
    }

    private static void appendCharactersPresent(StringBuilder sb, CharacterState character,
                                                 WorldState world) {
        sb.append("== Characters Present ==\n");
        List<CharacterState> others = world.charactersInRoom(character.currentRoom()).stream()
            .filter(c -> !c.agentId().equals(character.agentId()))
            .toList();
        if (others.isEmpty()) {
            sb.append("You are alone.\n");
        } else {
            for (CharacterState other : others) {
                sb.append("- ").append(other.name())
                  .append(" (at position ").append(other.x()).append(")\n");
            }
        }
        sb.append("\n");
    }

    private static void appendInventory(StringBuilder sb, CharacterState character) {
        sb.append("== Your Inventory ==\n");
        if (character.inventory().isEmpty()) {
            sb.append("You are carrying nothing.\n");
        } else {
            for (String item : character.inventory()) {
                sb.append("- ").append(item).append("\n");
            }
        }
        sb.append("\n");
    }

    private static void appendRecentActivity(StringBuilder sb, CharacterState character,
                                              WorldState world) {
        sb.append("== Recent Activity ==\n");
        List<ManorEvent> events = world.recentEvents(
            character.currentRoom(), RECENT_EVENT_LIMIT);
        if (events.isEmpty()) {
            sb.append("The room is quiet.\n");
        } else {
            for (ManorEvent event : events) {
                sb.append("- ").append(event.description()).append("\n");
            }
        }
    }
}
