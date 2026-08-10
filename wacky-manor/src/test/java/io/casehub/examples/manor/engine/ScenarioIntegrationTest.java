package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionResult;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.Scene;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioIntegrationTest {

    private WorldState world;
    private ActionResolver actionResolver;
    private TriggerEvaluator triggerEvaluator;
    private SceneDirector sceneDirector;
    private List<String> narratorLog;
    private List<String> sceneDialogue;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        actionResolver = new ActionResolver();
        triggerEvaluator = new TriggerEvaluator(MansionLoader.loadTriggers());
        Map<String, Scene> scenes = MansionLoader.loadScenes();
        sceneDirector = new SceneDirector(scenes);
        narratorLog = new ArrayList<>();
        sceneDialogue = new ArrayList<>();
    }

    @Test
    void full_tea_poisoning_scenario_golden_path() {
        // -- Act 1: Hooded Claw enters kitchen, discovers poison --
        var hc = world.character("hooded-claw");

        var moveResult = actionResolver.resolve(hc,
            new Action(ActionType.MOVE, "kitchen", null), world);
        assertThat(moveResult).isInstanceOf(ActionResult.MovedToRoom.class);
        assertThat(hc.currentRoom()).isEqualTo("kitchen");

        var triggerResult = triggerEvaluator.evaluate(world);
        assertThat(triggerResult.narratorEvents()).hasSize(1);
        assertThat(triggerResult.narratorEvents().get(0)).contains("DIABOLICAL");
        assertThat(world.visibleObjects("kitchen", "hooded-claw"))
            .anyMatch(o -> o.id().equals("poison"));

        // -- Act 2: Hooded Claw takes poison --
        hc.setX(0.7);
        var takeResult = actionResolver.resolve(hc,
            new Action(ActionType.TAKE, "poison", null), world);
        assertThat(takeResult).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(hc.hasItem("rat-poison")).isTrue();

        // -- Act 3: Move Penelope and Ant Hill Mob to ballroom --
        world.moveCharacter("penelope-pitstop", "ballroom");
        world.moveCharacter("ant-hill-mob", "ballroom");

        // -- Act 4: Hooded Claw enters ballroom — tea scene triggers --
        var moveBallroom = actionResolver.resolve(hc,
            new Action(ActionType.MOVE, "ballroom", null), world);
        assertThat(moveBallroom).isInstanceOf(ActionResult.MovedToRoom.class);

        var teaTrigger = triggerEvaluator.evaluate(world);
        assertThat(teaTrigger.hasSceneStart()).isTrue();
        assertThat(teaTrigger.sceneId()).isEqualTo("tea-poisoning");

        // -- Act 5: Tea scene runs — Ant Hill Mob foils the poisoning --
        sceneDirector.runScene("tea-poisoning", world,
            (charId, prompt) -> {
                String response = charId + " responds to: " + prompt.substring(0, Math.min(30, prompt.length()));
                sceneDialogue.add(response);
                return response;
            },
            narratorLog::add);

        assertThat(narratorLog).anyMatch(n -> n.contains("FREEZE mercury"));
        assertThat(narratorLog).anyMatch(n -> n.contains("Ant Hill Mob"));
        assertThat(hc.hasItem("rat-poison")).isFalse();
        assertThat(world.isSceneCompleted("tea-poisoning")).isTrue();
        assertThat(sceneDialogue).isNotEmpty();

        // -- Act 6: Scenario completes --
        var completeTrigger = triggerEvaluator.evaluate(world);
        assertThat(world.isScenarioComplete()).isTrue();
    }

    @Test
    void blubber_foil_fires_when_ant_hill_mob_absent() {
        var hc = world.character("hooded-claw");

        world.moveCharacter("hooded-claw", "kitchen");
        triggerEvaluator.evaluate(world);
        hc.setX(0.7);
        actionResolver.resolve(hc, new Action(ActionType.TAKE, "poison", null), world);

        world.moveCharacter("penelope-pitstop", "ballroom");
        world.moveCharacter("hooded-claw", "ballroom");

        var teaTrigger = triggerEvaluator.evaluate(world);
        assertThat(teaTrigger.hasSceneStart()).isTrue();

        sceneDirector.runScene("tea-poisoning", world,
                               (charId, prompt) -> "test response",
                               narratorLog::add);

        assertThat(narratorLog).anyMatch(n -> n.contains("Blubber Bear"));
        assertThat(narratorLog).noneMatch(n -> n.contains("Ant Hill Mob are here"));
        assertThat(hc.hasItem("rat-poison")).isFalse();

        triggerEvaluator.evaluate(world);
        assertThat(world.isScenarioComplete()).isTrue();}

    @Test
    void full_item_chain_medal_to_key_to_cabinet() {
        var dastardly = world.character("dick-dastardly");

        // Muttley is now a character — brass-key exchange is LLM-driven.
        // Test the mechanical part: brass-key → cabinet → recipe-cards.
        dastardly.addItem("brass-key");

        world.moveCharacter("dick-dastardly", "kitchen");
        dastardly.setX(0.3);

        var cabinet = actionResolver.resolve(dastardly,
                                             new Action(ActionType.INTERACT, "cabinet", "brass-key"), world);
        assertThat(cabinet).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(dastardly.hasItem("old-recipe-cards")).isTrue();
        assertThat(dastardly.hasItem("brass-key")).isFalse();
    }

    @Test
    void poison_visible_to_penelope_in_kitchen() {
        world.moveCharacter("penelope-pitstop", "kitchen");
        var penelopeObjects = world.visibleObjects("kitchen", "penelope-pitstop");
        assertThat(penelopeObjects).anyMatch(o -> o.id().equals("poison"));
    }

    @Test
    void trigger_does_not_fire_without_full_conditions() {
        world.character("hooded-claw").addItem("rat-poison");
        world.moveCharacter("penelope-pitstop", "ballroom");

        var result = triggerEvaluator.evaluate(world);
        assertThat(result.hasSceneStart()).isFalse();
    }

    @Test
    void inactive_character_excluded_from_room_queries() {
        int entranceCount = world.charactersInRoom("entrance-hall").size();

        world.markCharacterInactive("peter-perfect");
        world.markCharacterInactive("dick-dastardly");

        assertThat(world.charactersInRoom("entrance-hall")).hasSize(entranceCount - 2);
        assertThat(world.charactersInRoom("entrance-hall"))
                .noneMatch(c -> c.agentId().equals("peter-perfect"))
                .noneMatch(c -> c.agentId().equals("dick-dastardly"));
    }
}
