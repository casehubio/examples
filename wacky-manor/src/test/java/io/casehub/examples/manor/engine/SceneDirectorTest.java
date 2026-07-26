package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.Scene;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SceneDirectorTest {

    private WorldState world;
    private SceneDirector director;
    private List<String> narratorOutput;
    private List<String> llmCalls;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        Map<String, Scene> scenes = MansionLoader.loadScenes();
        director = new SceneDirector(scenes);
        narratorOutput = new ArrayList<>();
        llmCalls = new ArrayList<>();
    }

    @Test
    void scene_produces_narrator_output_for_each_beat() {
        setupTeaSceneConditions();

        director.runScene("tea-poisoning", world,
            (charId, prompt) -> {
                llmCalls.add(charId + ": " + prompt);
                return "Test response from " + charId;
            },
            narratorOutput::add);

        assertThat(narratorOutput).hasSizeGreaterThanOrEqualTo(3);
        assertThat(narratorOutput.get(0)).contains("FREEZE mercury");
    }

    @Test
    void scene_calls_llm_for_each_prompted_character() {
        setupTeaSceneConditions();

        director.runScene("tea-poisoning", world,
            (charId, prompt) -> {
                llmCalls.add(charId + ": " + prompt);
                return "Test response";
            },
            narratorOutput::add);

        assertThat(llmCalls).anyMatch(p -> p.contains("pour"));
        assertThat(llmCalls).anyMatch(p -> p.contains("Sneekly"));
    }

    @Test
    void mob_foil_fires_when_mob_in_ballroom() {
        setupTeaSceneConditions();
        world.moveCharacter("ant-hill-mob", "ballroom");

        director.runScene("tea-poisoning", world,
            (charId, prompt) -> {
                llmCalls.add(charId + ": " + prompt);
                return "Test response";
            },
            narratorOutput::add);

        assertThat(narratorOutput).anyMatch(n -> n.contains("Ant Hill Mob"));
        assertThat(world.character("hooded-claw").hasItem("rat-poison")).isFalse();
    }

    @Test
    void blubber_foil_fires_as_fallback_when_mob_absent() {
        setupTeaSceneConditions();

        director.runScene("tea-poisoning", world,
            (charId, prompt) -> {
                llmCalls.add(charId + ": " + prompt);
                return "Test response";
            },
            narratorOutput::add);

        assertThat(narratorOutput).anyMatch(n -> n.contains("Blubber Bear"));
        assertThat(world.character("hooded-claw").hasItem("rat-poison")).isFalse();
    }

    @Test
    void scene_marks_completed_after_all_beats() {
        setupTeaSceneConditions();

        director.runScene("tea-poisoning", world,
            (charId, prompt) -> "Test response",
            narratorOutput::add);

        assertThat(world.isSceneCompleted("tea-poisoning")).isTrue();
    }

    @Test
    void aside_beats_record_events_as_aside_type() {
        setupTeaSceneConditions();

        director.runScene("tea-poisoning", world,
            (charId, prompt) -> "villain monologue",
            narratorOutput::add);

        var asideEvents = world.recentEvents("ballroom", 20).stream()
            .filter(e -> "aside".equals(e.type()))
            .toList();
        assertThat(asideEvents).isNotEmpty();
    }

    @Test
    void mob_foil_preferred_over_blubber_when_both_present() {
        setupTeaSceneConditions();
        world.moveCharacter("ant-hill-mob", "ballroom");

        director.runScene("tea-poisoning", world,
            (charId, prompt) -> "Test response",
            narratorOutput::add);

        assertThat(narratorOutput).anyMatch(n -> n.contains("Ant Hill Mob"));
        assertThat(narratorOutput).noneMatch(n -> n.contains("Blubber Bear"));
    }

    private void setupTeaSceneConditions() {
        world.moveCharacter("penelope-pitstop", "ballroom");
        world.moveCharacter("hooded-claw", "ballroom");
        world.character("hooded-claw").addItem("rat-poison");
    }
}
