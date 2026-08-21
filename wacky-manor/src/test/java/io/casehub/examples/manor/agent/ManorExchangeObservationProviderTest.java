package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.affordance.AffordanceRenderer;
import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.engine.WorldState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManorExchangeObservationProviderTest {

    private WorldState world;
    private final AffordanceRenderer renderer = new AffordanceRenderer();

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
    }

    @Test
    void exchangeProvider_includes_location_others_dialogue() {
        var character = world.character("penelope-pitstop");
        var provider = new ManorExchangeObservationProvider(character, "Hehehehe!", world);
        var sections = provider.worldSections();
        var rendered = renderer.renderObservation(sections);
        assertThat(rendered).contains("== Location ==");
        assertThat(rendered).contains("Entrance Hall");
        assertThat(rendered).contains("== They said ==");
        assertThat(rendered).contains("Hehehehe!");
    }

    @Test
    void exchangeProvider_includes_others_present() {
        var character = world.character("penelope-pitstop");
        var provider = new ManorExchangeObservationProvider(character, "Hello!", world);
        var sections = provider.worldSections();
        var rendered = renderer.renderObservation(sections);
        assertThat(rendered).contains("== Others Present ==");
    }
}
