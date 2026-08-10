package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.agent.NarrativeEventBuilder;
import io.casehub.examples.manor.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutonomousScenarioTest {

    private WorldState world;
    private ActionResolver resolver;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        resolver = new ActionResolver();
    }

    @Test
    void autonomous_poison_scenario_end_to_end() {
        var hc = world.character("hooded-claw");

        var moveResult = resolver.resolve(hc,
            new Action(ActionType.MOVE, "kitchen", null), world);
        assertThat(moveResult).isInstanceOf(ActionResult.MovedToRoom.class);
        assertThat(hc.currentRoom()).isEqualTo("kitchen");

        var visibleInKitchen = world.visibleObjects("kitchen", "hooded-claw");
        assertThat(visibleInKitchen).anyMatch(o -> o.id().equals("poison"));

        var takeResult = resolver.resolve(hc,
            new Action(ActionType.TAKE, "poison", null), world);
        assertThat(takeResult).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(hc.hasItem("rat-poison")).isTrue();
        assertThat(world.isObjectTaken("poison")).isTrue();

        assertThat(world.visibleObjects("kitchen", "hooded-claw"))
            .noneMatch(o -> o.id().equals("poison"));

        resolver.resolve(hc, new Action(ActionType.MOVE, "ballroom", null), world);

        var useResult = resolver.resolve(hc,
            new Action(ActionType.USE, "tea-service", "rat-poison"), world);
        assertThat(useResult).isInstanceOf(ActionResult.Success.class);
        assertThat(hc.hasItem("rat-poison")).isFalse();
        assertThat(world.hasEffect("tea-service", "rat-poison")).isTrue();
    }

    @Test
    void autonomous_mode_does_not_fire_triggers() {
        var hc = world.character("hooded-claw");
        resolver.resolve(hc, new Action(ActionType.MOVE, "kitchen", null), world);

        assertThat(world.hasTriggerFired("poison-discovery")).isFalse();
    }

    @Test
    void narrative_events_are_vague_for_suspicious_actions() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "kitchen");

        var takeResult = resolver.resolve(hc,
            new Action(ActionType.TAKE, "poison", null), world);
        String narrative = NarrativeEventBuilder.describe(hc,
            new Action(ActionType.TAKE, "poison", null), takeResult);
        assertThat(narrative).isEqualTo("The Hooded Claw (as Sneekly) picked up something.");
        assertThat(narrative).doesNotContain("poison");
        assertThat(narrative).doesNotContain("Rat Poison");
    }

    @Test
    void turn_limit_terminates_scenario() {
        var penelope = world.character("penelope-pitstop");
        int turnCount = 0;
        int maxTurns = 3;

        while (!world.isScenarioComplete()) {
            resolver.resolve(penelope,
                new Action(ActionType.LOOK, null, null), world);
            turnCount++;
            if (turnCount >= maxTurns) {
                world.setScenarioComplete(CompletionReason.DAWN);
            }
        }

        assertThat(world.isScenarioComplete()).isTrue();
        assertThat(world.completionReason()).isEqualTo(CompletionReason.DAWN);
    }

    @Test
    void poisoning_triggers_completion() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "kitchen");
        resolver.resolve(hc, new Action(ActionType.TAKE, "poison", null), world);
        world.moveCharacter("hooded-claw", "ballroom");
        resolver.resolve(hc, new Action(ActionType.USE, "tea-service", "rat-poison"), world);

        assertThat(world.hasEffect("tea-service", "rat-poison")).isTrue();

        world.setScenarioComplete(CompletionReason.DAWN);
        assertThat(world.completionReason()).isEqualTo(CompletionReason.DAWN);
    }

    @Test
    void action_result_text_used_for_last_action_result() {
        var hc = world.character("hooded-claw");
        var result = resolver.resolve(hc,
            new Action(ActionType.MOVE, "kitchen", null), world);
        hc.setLastActionResult(result.text());
        assertThat(hc.lastActionResult()).isEqualTo("You moved to Kitchen.");
    }
}
