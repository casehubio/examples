package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.ObservationResult;
import io.casehub.blocks.summarisation.observation.PartitionedDrain;
import io.casehub.blocks.summarisation.observation.affordance.AffordanceRenderer;
import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.engine.WorldState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ManorWorldObservationProviderTest {

    private WorldState world;
    private PartitionedDrain<String> emptyDrain;
    private final AffordanceRenderer renderer = new AffordanceRenderer();

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        emptyDrain = new PartitionedDrain<>(ObservationResult.empty(0), Map.of());
    }

    @Test
    void worldSections_includes_location_exits_objects_characters() {
        var character = world.character("penelope-pitstop");
        var provider = new ManorWorldObservationProvider(character, world, emptyDrain, Set.of());
        var sections = provider.worldSections();
        var rendered = renderer.renderObservation(sections);
        assertThat(rendered).contains("== Current Location ==");
        assertThat(rendered).contains("Entrance Hall");
        assertThat(rendered).contains("== Exits ==");
        assertThat(rendered).contains("== Visible Objects ==");
        assertThat(rendered).contains("== Characters Present ==");
    }

    @Test
    void worldSections_non_perceptive_has_no_keen_observations() {
        var character = world.character("penelope-pitstop");
        var provider = new ManorWorldObservationProvider(character, world, emptyDrain, Set.of());
        var sections = provider.worldSections();
        var rendered = renderer.renderObservation(sections);
        assertThat(rendered).doesNotContain("== Keen Observations ==");
    }

    @Test
    void worldSections_perceptive_has_no_directed_dialogue() {
        var character = world.character("penelope-pitstop");
        var provider = new ManorWorldObservationProvider(character, world, emptyDrain, Set.of("perception"));
        var sections = provider.worldSections();
        var rendered = renderer.renderObservation(sections);
        assertThat(rendered).doesNotContain("== Directed to You ==");
    }

    @Test
    void worldSections_shows_alone_when_no_others() {
        var character = world.character("penelope-pitstop");
        var alone = MansionLoader.loadWorld();
        alone.charactersInRoom("entrance-hall").stream()
             .filter(c -> !c.agentId().equals("penelope-pitstop"))
             .forEach(c -> c.setCurrentRoom("kitchen"));
        var provider = new ManorWorldObservationProvider(character, alone, emptyDrain, Set.of());
        var sections = provider.worldSections();
        var rendered = renderer.renderObservation(sections);
        assertThat(rendered).contains("You are alone.");
    }
}
