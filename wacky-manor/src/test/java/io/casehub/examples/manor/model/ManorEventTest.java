package io.casehub.examples.manor.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ManorEventTest {

    @Test
    void enrichedManorEvent_carriesActionMetadata() {
        var event = new ManorEvent.Action(Instant.now(), "hooded-claw", "kitchen",
                "Sneekly picked up something.", ActionType.TAKE, "poison", null, null);
        assertThat(event.actionType()).isEqualTo(ActionType.TAKE);
        assertThat(event.target()).isEqualTo("poison");
        assertThat(event.withItem()).isNull();
        assertThat(event.departureRoom()).isNull();
    }

    @Test
    void dialogueEvent_hasNoActionFields() {
        var event = new ManorEvent.Dialogue(Instant.now(), "penelope", "ballroom",
                "Why, hello darlin'!");
        assertThat(event.actionType()).isNull();
        assertThat(event.target()).isNull();
        assertThat(event.withItem()).isNull();
        assertThat(event.departureRoom()).isNull();
    }

    @Test
    void moveEvent_carriesDepartureRoom() {
        var event = new ManorEvent.Action(Instant.now(), "penelope", "kitchen",
                "Penelope walked into the Kitchen.", ActionType.MOVE, "kitchen", null, "entrance-hall");
        assertThat(event.departureRoom()).isEqualTo("entrance-hall");
    }

    @Test
    void detailedDescription_null_by_default() {
        var event = new ManorEvent.Action(Instant.now(), "hooded-claw", "kitchen",
                                   "Sneekly picked up something.", ActionType.TAKE, "poison", null, null);
        assertThat(event.detailedDescription()).isNull();
    }

    @Test
    void detailedDescription_carried_when_set() {
        var event = new ManorEvent.Action(Instant.now(), "hooded-claw", "kitchen",
                                   "Sneekly picked up something.", ActionType.TAKE, "poison", null, null,
                                   "Sneekly carefully pocketed the rat poison.", false);
        assertThat(event.detailedDescription()).isEqualTo("Sneekly carefully pocketed the rat poison.");
    }

    @Test
    void dialogueTarget_null_by_default() {
        var event = new ManorEvent.Dialogue(Instant.now(), "hc", "hall", "Hello");
        assertThat(event.dialogueTarget()).isNull();
    }
}
