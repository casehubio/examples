package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.Room;
import io.casehub.neocortex.cognitive.ConfidenceOrigin;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DialogueExtractionTest {

    @Test
    void directedDialogue_targetGetsStated() {
        var world = buildWorld(
                character("speaker", "Library"),
                character("target", "Library"));
        var listeners = ScenarioOrchestrator.determineListeners(
                "speaker", "target", "Library", false, world);
        assertThat(listeners).hasSize(1);
        assertThat(listeners.get(0).agentId()).isEqualTo("target");
        assertThat(listeners.get(0).confidenceOrigin()).isEqualTo(ConfidenceOrigin.STATED);
    }

    @Test
    void directedDialogue_perceptionTagOverhears() {
        var world = buildWorld(
                character("speaker", "Library"),
                character("target", "Library"),
                characterWithTags("eavesdropper", "Library", Set.of("perception")));
        var listeners = ScenarioOrchestrator.determineListeners(
                "speaker", "target", "Library", false, world);
        assertThat(listeners).hasSize(2);
        assertThat(listeners).extracting(ScenarioOrchestrator.ListenerInfo::agentId)
                .containsExactlyInAnyOrder("target", "eavesdropper");
        var eavesdropperInfo = listeners.stream()
                .filter(l -> l.agentId().equals("eavesdropper")).findFirst().orElseThrow();
        assertThat(eavesdropperInfo.confidenceOrigin()).isEqualTo(ConfidenceOrigin.INFERRED);
    }

    @Test
    void directedDialogue_noPerceptionNoOverhear() {
        var world = buildWorld(
                character("speaker", "Library"),
                character("target", "Library"),
                character("bystander", "Library"));
        var listeners = ScenarioOrchestrator.determineListeners(
                "speaker", "target", "Library", false, world);
        assertThat(listeners).hasSize(1);
        assertThat(listeners.get(0).agentId()).isEqualTo("target");
    }

    @Test
    void directedDialogue_differentRoomNotIncluded() {
        var world = buildWorld(
                character("speaker", "Library"),
                character("target", "Library"),
                characterWithTags("elsewhere", "Kitchen", Set.of("perception")));
        var listeners = ScenarioOrchestrator.determineListeners(
                "speaker", "target", "Library", false, world);
        assertThat(listeners).hasSize(1);
    }

    @Test
    void exchange_bothParticipantsGetStated_noOverhearing() {
        var world = buildWorld(
                character("initiator", "Library"),
                character("target", "Library"),
                characterWithTags("bystander", "Library", Set.of("perception")));
        var listeners = ScenarioOrchestrator.determineListeners(
                "initiator", "target", "Library", true, world);
        assertThat(listeners).hasSize(2);
        assertThat(listeners).extracting(ScenarioOrchestrator.ListenerInfo::agentId)
                .containsExactlyInAnyOrder("initiator", "target");
        assertThat(listeners).allMatch(l -> l.confidenceOrigin() == ConfidenceOrigin.STATED);
    }

    @Test
    void roomDialogue_allPresentGetStated() {
        var world = buildWorld(
                character("speaker", "Library"),
                character("alice", "Library"),
                character("bob", "Library"));
        var listeners = ScenarioOrchestrator.determineListeners(
                "speaker", null, "Library", false, world);
        assertThat(listeners).hasSize(2);
        assertThat(listeners).extracting(ScenarioOrchestrator.ListenerInfo::agentId)
                .containsExactlyInAnyOrder("alice", "bob");
        assertThat(listeners).allMatch(l -> l.confidenceOrigin() == ConfidenceOrigin.STATED);
    }

    @Test
    void extractable_normalDialogue() {
        assertThat(ScenarioOrchestrator.isExtractableDialogue(
                "The poison is hidden in the Ballroom")).isTrue();
    }

    @Test
    void notExtractable_purelySounds() {
        assertThat(ScenarioOrchestrator.isExtractableDialogue("Hehehehehehe!")).isFalse();
    }

    @Test
    void notExtractable_singleWord() {
        assertThat(ScenarioOrchestrator.isExtractableDialogue("SQUEAK!")).isFalse();
    }

    @Test
    void notExtractable_null() {
        assertThat(ScenarioOrchestrator.isExtractableDialogue(null)).isFalse();
    }

    @Test
    void notExtractable_blank() {
        assertThat(ScenarioOrchestrator.isExtractableDialogue("   ")).isFalse();
    }

    @Test
    void extractable_shortButMeaningful() {
        assertThat(ScenarioOrchestrator.isExtractableDialogue(
                "I saw Dick steal it")).isTrue();
    }

    // --- Test helpers ---

    private static CharacterState character(String agentId, String room) {
        return new CharacterState(agentId, agentId, room, 0, List.of());
    }

    private static CharacterState characterWithTags(String agentId, String room, Set<String> tags) {
        var cs = new CharacterState(agentId, agentId, room, 0, List.of());
        cs.setCapabilityTags(tags);
        return cs;
    }

    private static WorldState buildWorld(CharacterState... characters) {
        var rooms = new HashMap<String, Room>();
        var chars = new HashMap<String, CharacterState>();
        for (var c : characters) {
            chars.put(c.agentId(), c);
            rooms.putIfAbsent(c.currentRoom(),
                    new Room(c.currentRoom(), c.currentRoom(), "", List.of(), Map.of()));
        }
        return new WorldState(rooms, chars);
    }
}
