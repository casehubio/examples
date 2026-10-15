package io.casehub.examples.manor.experiment;

import io.casehub.examples.manor.agent.CharacterCognition;
import io.casehub.examples.manor.agent.ManorContextStrategy;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.examples.manor.agent.SocialConfig;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.caps.CapsEngine;
import io.casehub.neocortex.caps.SituationClassifier;
import io.casehub.neocortex.caps.llm.LlmSituationClassifier;
import io.casehub.neocortex.mindmap.MindMapQuery;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import io.casehub.neocortex.mindmap.intelligence.consolidation.DriveProfileDerivationPhase;
import io.casehub.platform.agent.AgentProvider;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end pipeline test: seeds real memories, runs real DriveProfileDerivationPhase
 * with the real LLM classifier, then verifies appraisal filtering produces correct
 * render decisions for real scenario event text.
 *
 * ~3-4 LLM calls, ~2 minutes. No server boot, no scenario run.
 */
@QuarkusTest
@Tag("llm-eval")
class DriveProfilePipelineTest {

    private static final String TENANT = "pipeline-test";
    private static final String AGENT_ID = "peter-perfect";

    private static final String SCENARIO_SPOKE_QUIETLY =
            "Peter Perfect spoke quietly with penelope-pitstop.";
    private static final String SCENARIO_WARMTH_ASIDE =
            "She asked for your help, Hartwell. Not Dastardly's vault-cracking bravado. " +
            "Your methodical, over-thorough approach.";

    @Inject AgentProvider agentProvider;
    @Inject CapsEngine capsEngine;

    private SocialConfig socialConfig;
    private InMemoryMindMapStore store;
    private SituationClassifier classifier;

    @BeforeEach
    void setUp() {
        socialConfig = ManorSocialConfigLoader.loadForProfile("GENERIC").get(AGENT_ID);
        store = new InMemoryMindMapStore();
        classifier = new LlmSituationClassifier(agentProvider, capsEngine.topology());
    }

    @Test
    void fullPipeline_deriveProfiles_thenFilter_gallantryRendersInSocialEvent() {
        seedMindMap();

        var phase = new DriveProfileDerivationPhase(store, classifier, capsEngine.topology());
        phase.run(TENANT, List.of());

        var profileNodes = store.search(MindMapQuery.of(TENANT, 100).withType(SubgraphTypes.COGNITIVE))
                .stream()
                .filter(n -> n.traits().contains("DriveProfileInferred"))
                .filter(n -> AGENT_ID.equals(n.properties().get("agent-id")))
                .toList();

        System.out.println("=== Derived profiles ===");
        for (var node : profileNodes) {
            System.out.println(node.properties().get("drive-type") + ": " +
                    node.properties().get("inferred-inputs"));
        }

        assertThat(profileNodes).as("Should derive profiles for PP drives")
                .hasSizeGreaterThanOrEqualTo(2);

        var gallantryProfile = profileNodes.stream()
                .filter(n -> "gallantry".equals(n.properties().get("drive-type")))
                .findFirst();
        assertThat(gallantryProfile).as("Gallantry profile should exist").isPresent();

        String gallantryInputs = gallantryProfile.get().properties().get("inferred-inputs");
        System.out.println("Gallantry inputs: " + gallantryInputs);

        var cognition = new CharacterCognition(AGENT_ID, null, null,
                socialConfig, List.of(), null, new ManorContextStrategy(), null, null,
                TENANT, store, null, classifier);
        var character = new CharacterState(AGENT_ID, AGENT_ID, "Grand Hall", 0.0, List.of());

        character.setLastActionResult(SCENARIO_SPOKE_QUIETLY);
        var sectionsQuiet = cognition.renderCognitiveSections(character,
                List.of("penelope-pitstop"), Map.of("penelope-pitstop", "Clara Bellingham"));
        boolean gallantryInQuiet = sectionsContainDrive(sectionsQuiet, "gallantry", socialConfig);
        System.out.println("'spoke quietly' → gallantry rendered: " + gallantryInQuiet);

        character.setLastActionResult(SCENARIO_WARMTH_ASIDE);
        var sectionsWarmth = cognition.renderCognitiveSections(character,
                List.of("penelope-pitstop"), Map.of("penelope-pitstop", "Clara Bellingham"));
        boolean gallantryInWarmth = sectionsContainDrive(sectionsWarmth, "gallantry", socialConfig);
        System.out.println("'she asked for help' → gallantry rendered: " + gallantryInWarmth);

        assertThat(gallantryInWarmth)
                .as("Gallantry should render in rich social event")
                .isTrue();
    }

    private void seedMindMap() {
        var sgId = store.createSubgraph(
                new SubgraphInput("cognitive-" + AGENT_ID, SubgraphTypes.COGNITIVE, null), TENANT);

        for (var drive : socialConfig.drives()) {
            store.addNode(NodeInput.of(drive.type(), sgId)
                    .withProperties(Map.of(
                            "cognitiveKind", "drive-intensity",
                            "agent-id", AGENT_ID,
                            "drive-type", drive.type(),
                            "intensity", String.valueOf(drive.intensity()),
                            "description", drive.description())), TENANT);
        }

        for (var mem : socialConfig.formationMemories()) {
            var driveTypes = new ArrayList<String>();
            driveTypes.add("formation");
            for (var ann : mem.annotations()) {
                if (!driveTypes.contains(ann.drive())) driveTypes.add(ann.drive());
            }
            store.addNode(NodeInput.of(mem.episode(), sgId)
                    .withProperties(Map.of(
                            "cognitiveKind", "formative-experience",
                            "agent-id", AGENT_ID,
                            "situation-types", String.join(",", driveTypes))), TENANT);
        }
    }

    private boolean sectionsContainDrive(List<?> sections, String driveType, SocialConfig config) {
        var driveDesc = config.drives().stream()
                .filter(d -> d.type().equals(driveType))
                .map(SocialConfig.Drive::description)
                .findFirst().orElse("");
        for (var section : sections) {
            if (section.toString().contains(driveDesc) && !driveDesc.isEmpty()) return true;
        }
        return false;
    }
}
