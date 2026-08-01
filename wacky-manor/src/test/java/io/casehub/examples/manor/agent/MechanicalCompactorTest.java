package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.EventLevel;
import io.casehub.blocks.summarisation.LevelEvent;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.ManorEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MechanicalCompactorTest {

    static final EventLevel MANOR = new EventLevel("manor", 0);
    final MechanicalCompactor compactor = new MechanicalCompactor();

    private LevelEvent<ManorEvent> moveEvent(String charId, String room,
                                              String departure, long ts) {
        return new LevelEvent<>(new ManorEvent(Instant.ofEpochMilli(ts), "action", charId,
                room, charId + " moved to " + room, ActionType.MOVE, room, null, departure),
                ts, MANOR);
    }

    private LevelEvent<ManorEvent> dialogueEvent(String charId, String room,
                                                   String text, long ts) {
        return new LevelEvent<>(new ManorEvent(Instant.ofEpochMilli(ts), "dialogue", charId,
                room, charId + ": " + text), ts, MANOR);
    }

    @Test
    void positionSupersession_keepsOnlyLatestMovePerCharacter() {
        var events = List.of(
                moveEvent("penelope", "kitchen", "entrance-hall", 100),
                moveEvent("penelope", "ballroom", "kitchen", 200));
        var result = compactor.compact(events);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).payload().room()).isEqualTo("ballroom");
    }

    @Test
    void positionSupersession_differentCharactersKeptSeparately() {
        var events = List.of(
                moveEvent("penelope", "kitchen", "entrance-hall", 100),
                moveEvent("hooded-claw", "ballroom", "kitchen", 200));
        var result = compactor.compact(events);
        assertThat(result).hasSize(2);
    }

    @Test
    void emptyInput_returnsEmpty() {
        assertThat(compactor.compact(List.of())).isEmpty();
    }

    @Test
    void singleEvent_passesThrough() {
        var events = List.of(dialogueEvent("penelope", "kitchen", "Hello!", 100));
        assertThat(compactor.compact(events)).hasSize(1);
    }

    @Test
    void inventorySupersession_laterTakeSameObjectWins() {
        var events = List.of(
                new LevelEvent<>(new ManorEvent(Instant.ofEpochMilli(100), "action", "penelope",
                        "kitchen", "Penelope picked up something.", ActionType.TAKE, "brass-key", null, null),
                        100, MANOR),
                new LevelEvent<>(new ManorEvent(Instant.ofEpochMilli(200), "action", "hooded-claw",
                        "kitchen", "Sneekly picked up something.", ActionType.TAKE, "brass-key", null, null),
                        200, MANOR));
        var result = compactor.compact(events);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).payload().characterId()).isEqualTo("hooded-claw");
    }

    @Test
    void objectStateSupersession_laterInteractSameObjectWins() {
        var events = List.of(
                new LevelEvent<>(new ManorEvent(Instant.ofEpochMilli(100), "action", "penelope",
                        "kitchen", "Penelope interacted with Cabinet.", ActionType.INTERACT, "cabinet", "brass-key", null),
                        100, MANOR),
                new LevelEvent<>(new ManorEvent(Instant.ofEpochMilli(200), "action", "peter",
                        "kitchen", "Peter used something on Cabinet.", ActionType.USE, "cabinet", "oil", null),
                        200, MANOR));
        var result = compactor.compact(events);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).payload().characterId()).isEqualTo("peter");
    }

    @Test
    void duplicateDialogue_keepsFirstOccurrence() {
        var events = List.of(
                dialogueEvent("penelope", "kitchen", "Hello darlin'!", 100),
                dialogueEvent("penelope", "kitchen", "Hello darlin'!", 200));
        var result = compactor.compact(events);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).timestamp()).isEqualTo(100);
    }

    @Test
    void mixedEvents_dialogueAndActionsCompactIndependently() {
        var events = List.of(
                dialogueEvent("penelope", "kitchen", "Hello!", 100),
                moveEvent("hooded-claw", "kitchen", "entrance-hall", 150),
                dialogueEvent("hooded-claw", "kitchen", "Greetings!", 200),
                moveEvent("hooded-claw", "ballroom", "kitchen", 300));
        var result = compactor.compact(events);
        assertThat(result).hasSize(3);
        assertThat(result.stream().filter(e -> e.payload().actionType() == ActionType.MOVE).count())
                .isEqualTo(1);
    }
}
