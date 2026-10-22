package io.casehub.examples.manor.agent;

import io.casehub.agency.summarisation.observation.affordance.ObservationSection;
import io.casehub.examples.manor.model.CharacterState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the scripted-mode observation path includes cognitive
 * sections (drives, beliefs, personality) — the same sections that the
 * autonomous-mode path renders.
 *
 * Regression guard: CharacterAgentLoop previously built observations
 * via ObservationBuilder.buildObservation() which bypassed cognitive
 * sections entirely. Phase 1/2 drive descriptions, situation priming,
 * and somatic discrimination were invisible in scripted mode.
 */
class ScriptedModeObservationTest {

    @Test
    void scriptedModeObservationIncludesCognitiveSections() {
        var socialConfig = ManorSocialConfigLoader.loadForProfile("generic").get("peter-perfect");
        var cognition = new CharacterCognition("peter-perfect", null, null,
            socialConfig, List.of());
        var character = new CharacterState("peter-perfect", "James Hartwell",
            "Grand Hall", 0.0, List.of());

        var sections = cognition.renderCognitiveSections(character, List.of(), Map.of());

        var headers = sections.stream().map(ObservationSection::header).toList();
        assertThat(headers).contains("What Pulls at You");
    }

    @Test
    void scriptedModeObservationMatchesAutonomousMode() {
        var socialConfig = ManorSocialConfigLoader.loadForProfile("generic").get("peter-perfect");
        var cognition = new CharacterCognition("peter-perfect", null, null,
            socialConfig, List.of());
        var character = new CharacterState("peter-perfect", "James Hartwell",
            "Grand Hall", 0.0, List.of());

        var cognitiveSections = cognition.renderCognitiveSections(character, List.of(), Map.of());

        var cognitiveHeaders = cognitiveSections.stream()
            .map(ObservationSection::header).toList();

        assertThat(cognitiveHeaders)
            .as("Scripted mode must include the same cognitive sections as autonomous mode")
            .contains("What Pulls at You");

        var whatPulls = cognitiveSections.stream()
            .filter(s -> "What Pulls at You".equals(s.header()))
            .findFirst().orElseThrow();
        var content = ((ObservationSection.TextBlock) whatPulls).content();
        assertThat(content).isNotBlank();
    }

    @Test
    void scriptedModeWithMindMapStoreUsesGeneratedDescriptions() {
        var store = new io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore();
        var agent = "peter-perfect";
        var tenant = "scripted-test";
        var configs = ManorSocialConfigLoader.loadForProfile("generic");
        var ppConfig = configs.get(agent);

        var sgId = store.createSubgraph(
            new io.casehub.neocortex.mindmap.SubgraphInput("cognitive-" + agent,
                io.casehub.neocortex.mindmap.SubgraphTypes.COGNITIVE, null), tenant);

        store.addNode(io.casehub.neocortex.mindmap.NodeInput.of("gallantry", sgId)
            .withProperties(Map.of("cognitiveKind", "drive-intensity", "agent-id", agent,
                "drive-type", "gallantry", "intensity", "0.9", "description", "fallback")), tenant);

        store.addNode(io.casehub.neocortex.mindmap.NodeInput.of("drive-description-gallantry", sgId)
            .withTraits(java.util.Set.of("DriveDescriptionGenerated"))
            .withProperties(Map.of("agent-id", agent, "drive-type", "gallantry",
                "generated-description", "LLM-generated somatic description for gallantry")), tenant);

        var cognition = new CharacterCognition(agent, null, null, ppConfig, List.of(),
            null, new ManorContextStrategy(), null, null,
            tenant, store, null, null);
        var character = new CharacterState(agent, "James Hartwell",
            "Grand Hall", 0.0, List.of());

        var sections = cognition.renderCognitiveSections(character, List.of(), Map.of());
        var whatPulls = sections.stream()
            .filter(s -> "What Pulls at You".equals(s.header()))
            .findFirst().orElseThrow();
        var content = ((ObservationSection.TextBlock) whatPulls).content();

        assertThat(content).contains("LLM-generated somatic description for gallantry");
        assertThat(content).doesNotContain("fallback");
    }
}
