package io.casehub.examples.manor.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TriggerEvaluatorTest {

    private WorldState world;
    private TriggerEvaluator evaluator;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        evaluator = new TriggerEvaluator(MansionLoader.loadTriggers());
    }

    @Test
    void narrator_event_fires_when_hooded_claw_enters_kitchen() {
        world.moveCharacter("hooded-claw", "kitchen");
        var result = evaluator.evaluate(world);

        assertThat(result.narratorEvents()).hasSize(1);
        assertThat(result.narratorEvents().get(0)).contains("DIABOLICAL");
    }

    @Test
    void once_trigger_does_not_fire_twice() {
        world.moveCharacter("hooded-claw", "kitchen");
        evaluator.evaluate(world);
        var result2 = evaluator.evaluate(world);
        assertThat(result2.narratorEvents()).isEmpty();
    }

    @Test
    void tea_scene_triggers_when_all_conditions_met() {
        world.moveCharacter("hooded-claw", "kitchen");
        evaluator.evaluate(world);

        world.character("hooded-claw").addItem("rat-poison");
        world.moveCharacter("hooded-claw", "ballroom");
        world.moveCharacter("penelope-pitstop", "ballroom");

        var result = evaluator.evaluate(world);
        assertThat(result.hasSceneStart()).isTrue();
        assertThat(result.sceneId()).isEqualTo("tea-poisoning");
    }

    @Test
    void tea_scene_does_not_trigger_without_poison() {
        world.moveCharacter("hooded-claw", "ballroom");
        world.moveCharacter("penelope-pitstop", "ballroom");

        var result = evaluator.evaluate(world);
        assertThat(result.hasSceneStart()).isFalse();
    }

    @Test
    void scenario_completes_when_scene_done() {
        world.markSceneCompleted("tea-poisoning");
        evaluator.evaluate(world);
        assertThat(world.isScenarioComplete()).isTrue();
    }

    @Test
    void no_triggers_fire_in_initial_state() {
        var result = evaluator.evaluate(world);
        assertThat(result.hasSceneStart()).isFalse();
        assertThat(result.narratorEvents()).isEmpty();
    }

    @Test
    void tea_scene_requires_all_three_conditions() {
        world.character("hooded-claw").addItem("rat-poison");
        world.moveCharacter("penelope-pitstop", "ballroom");

        var result = evaluator.evaluate(world);
        assertThat(result.hasSceneStart()).isFalse();
    }
}
