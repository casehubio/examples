package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.affordance.ObservationSection;
import io.casehub.blocks.summarisation.observation.affordance.WorldObservationProvider;
import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.Room;

import java.util.ArrayList;
import java.util.List;

public class ManorExchangeObservationProvider implements WorldObservationProvider {

    private final CharacterState character;
    private final String otherDialogue;
    private final WorldState world;

    public ManorExchangeObservationProvider(CharacterState character, String otherDialogue, WorldState world) {
        this.character = character;
        this.otherDialogue = otherDialogue;
        this.world = world;
    }

    @Override
    public List<ObservationSection> worldSections() {
        var sections = new ArrayList<ObservationSection>();
        Room room = world.room(character.currentRoom());
        sections.add(ObservationSection.text("Location", room.name()));

        List<CharacterState> others = world.charactersInRoom(character.currentRoom()).stream()
                .filter(c -> !c.agentId().equals(character.agentId())).toList();
        if (!others.isEmpty()) {
            sections.add(ObservationSection.items("Others Present", null,
                    others.stream().map(CharacterState::name).toList()));
        }
        sections.add(ObservationSection.text("They said", otherDialogue));
        return sections;
    }
}
