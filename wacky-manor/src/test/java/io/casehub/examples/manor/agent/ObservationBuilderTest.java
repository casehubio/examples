package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.engine.WorldState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationBuilderTest {

    private WorldState world;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
    }

    @Test
    void observation_includes_current_room() {
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope-pitstop"), world);
        assertThat(obs).contains("== Current Location ==");
        assertThat(obs).contains("Entrance Hall");
    }

    @Test
    void observation_shows_visible_objects() {
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope-pitstop"), world);
        assertThat(obs).contains("Coat Rack");
        assertThat(obs).contains("Guest Book");
        assertThat(obs).doesNotContain("Rat Poison");
    }

    @Test
    void hooded_claw_sees_poison_in_kitchen() {
        world.moveCharacter("hooded-claw", "kitchen");
        var obs = ObservationBuilder.buildObservation(
            world.character("hooded-claw"), world);
        assertThat(obs).contains("Rat Poison");
        assertThat(obs).contains("[can be picked up]");
    }

    @Test
    void penelope_does_not_see_poison() {
        world.moveCharacter("penelope-pitstop", "kitchen");
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope-pitstop"), world);
        assertThat(obs).doesNotContain("Rat Poison");
    }

    @Test
    void observation_lists_other_characters_in_room() {
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope-pitstop"), world);
        assertThat(obs).contains("== Characters Present ==");
        assertThat(obs).contains("The Hooded Claw");
        assertThat(obs).doesNotContain("Penelope Pitstop");
    }

    @Test
    void observation_shows_alone_when_no_others() {
        world.moveCharacter("penelope-pitstop", "kitchen");
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope-pitstop"), world);
        assertThat(obs).contains("You are alone.");
    }

    @Test
    void observation_shows_inventory() {
        var dastardly = world.character("dick-dastardly");
        var obs = ObservationBuilder.buildObservation(dastardly, world);
        assertThat(obs).contains("== Your Inventory ==");
        assertThat(obs).contains("fake-medal");
    }

    @Test
    void observation_shows_empty_inventory() {
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope-pitstop"), world);
        assertThat(obs).contains("You are carrying nothing.");
    }

    @Test
    void observation_includes_recent_events() {
        world.addEvent("dialogue", "hooded-claw", "entrance-hall",
            "Oh, my DEAR Miss Pitstop!");
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope-pitstop"), world);
        assertThat(obs).contains("== Recent Activity ==");
        assertThat(obs).contains("Oh, my DEAR Miss Pitstop!");
    }

    @Test
    void observation_shows_quiet_room_when_no_events() {
        world.moveCharacter("penelope-pitstop", "kitchen");
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope-pitstop"), world);
        assertThat(obs).contains("The room is quiet.");
    }

    @Test
    void observation_shows_interactable_hints() {
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope-pitstop"), world);
        assertThat(obs).contains("[interactable, requires: fake-medal]");
    }

    @Test
    void observation_limits_recent_events_to_five() {
        for (int i = 0; i < 8; i++) {
            world.addEvent("dialogue", "penelope-pitstop", "entrance-hall", "Event " + i);
        }
        var obs = ObservationBuilder.buildObservation(
            world.character("penelope-pitstop"), world);
        assertThat(obs).contains("Event 7");
        assertThat(obs).contains("Event 3");
        assertThat(obs).doesNotContain("Event 2");
    }
}
