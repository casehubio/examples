package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.affordance.ObservationSection;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.mindmap.MindMapQuery;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full pipeline-to-rendering test: seeds MindMapStore with consolidation
 * artifacts, renders through CharacterCognition, and asserts on what
 * the character actually sees.
 *
 * No LLM calls. No Quarkus. Fast iteration on the rendering pipeline.
 */
class PipelineToRenderingTest {

    private InMemoryMindMapStore store;
    private String sgId;
    private static final String TENANT = "render-test";
    private static final String AGENT = "peter-perfect";

    @BeforeEach
    void setUp() {
        store = new InMemoryMindMapStore();
        sgId = store.createSubgraph(
            new SubgraphInput("cognitive-" + AGENT, SubgraphTypes.COGNITIVE, null), TENANT);
    }

    @Test
    void generatedDescriptions_replaceHandAuthored() {
        seedDrive("gallantry", 0.9, "Hand-authored gallantry text");
        seedDrive("proving-worth", 0.7, "Hand-authored proving-worth text");
        seedGenerated("gallantry", "LLM somatic: heat behind sternum, shoulders squaring");
        seedGenerated("proving-worth", "LLM somatic: tension in hands, engine that won't idle");

        var content = renderWhatPulls();

        assertThat(content).contains("heat behind sternum");
        assertThat(content).contains("tension in hands");
        assertThat(content).doesNotContain("Hand-authored");
    }

    @Test
    void fallbackToHandAuthored_whenNoGenerated() {
        seedDrive("gallantry", 0.9, "Fallback gallantry description");

        var content = renderWhatPulls();

        assertThat(content).contains("Fallback gallantry description");
    }

    @Test
    void activatedDrives_appearsBeforeAmbient() {
        seedDrive("gallantry", 0.9, "Gallantry text");
        seedDrive("proving-worth", 0.7, "Proving-worth text");
        seedGenerated("gallantry", "Gallantry somatic");
        seedGenerated("proving-worth", "Proving-worth somatic");

        var section = renderInternalState(List.of("proving-worth"), "");

        var content = ((ObservationSection.TextBlock) section).content();
        assertThat(content.indexOf("Proving-worth somatic"))
            .as("Activated drive (proving-worth) should appear before ambient (gallantry)")
            .isLessThan(content.indexOf("Gallantry somatic"));
    }

    @Test
    void differentiationText_appearsInOutput() {
        seedDrive("gallantry", 0.9, "g");
        seedDrive("proving-worth", 0.7, "pw");
        seedGenerated("gallantry", "Gallantry somatic");
        seedGenerated("proving-worth", "Proving-worth somatic");
        seedCompetition("gallantry", "proving-worth",
            "How they differ:\nOne is gesture. The other is result.\nNotice which moved your hands.");

        var content = renderWhatPulls();

        assertThat(content).contains("How they differ");
        assertThat(content).contains("Notice which moved your hands");
    }

    @Test
    void situationPriming_surfacesRelevantMemory() {
        seedDrive("gallantry", 0.9, "g");
        seedGenerated("gallantry", "Gallantry somatic");
        seedFormativeMemory("The girl from school was in trouble", "gallantry");
        seedProfile("gallantry", "acceptance:0.7,sharing:0.5");

        io.casehub.neocortex.caps.SituationClassifier classifier = (desc, meta) -> {
            if (desc.contains("struggling"))
                return List.of(new io.casehub.neocortex.caps.SituationActivation("acceptance", 0.6));
            return List.of();
        };

        var section = renderInternalStateWithClassifier(
            List.of("gallantry"),
            "Clara is struggling with a heavy door",
            classifier);

        var content = ((ObservationSection.TextBlock) section).content();
        assertThat(content).contains("A memory surfaces");
        assertThat(content).contains("girl from school");
    }

    @Test
    void fullPipeline_allSignalsPresent() {
        seedDrive("gallantry", 0.9, "fallback");
        seedDrive("proving-worth", 0.7, "fallback");
        seedGenerated("gallantry", "Gallantry: heat behind sternum, shoulders squaring.");
        seedGenerated("proving-worth", "Proving-worth: tension in hands, engine that won't idle.");
        seedFormativeMemory("The girl from school was in trouble", "gallantry");
        seedProfile("gallantry", "acceptance:0.7");
        seedCompetition("gallantry", "proving-worth",
            "How they differ:\nOne pull is the gesture. The other is the result.\nNotice which one moved your hands.");

        io.casehub.neocortex.caps.SituationClassifier classifier = (desc, meta) -> {
            if (desc.contains("door"))
                return List.of(new io.casehub.neocortex.caps.SituationActivation("acceptance", 0.7));
            return List.of();
        };

        var section = renderInternalStateWithClassifier(
            List.of("gallantry"),
            "Clara is struggling with the door",
            classifier);

        var content = ((ObservationSection.TextBlock) section).content();

        assertThat(content)
            .as("Should have generated gallantry description")
            .contains("heat behind sternum");
        assertThat(content)
            .as("Should have generated proving-worth description")
            .contains("tension in hands");
        assertThat(content)
            .as("Should have situation-primed memory")
            .contains("A memory surfaces");
        assertThat(content)
            .as("Should have differentiation text")
            .contains("How they differ");
        assertThat(content)
            .as("Should NOT have hand-authored fallback")
            .doesNotContain("fallback");
    }

    // --- helpers ---

    private void seedDrive(String type, double intensity, String description) {
        store.addNode(NodeInput.of(type, sgId)
            .withProperties(Map.of("cognitiveKind", "drive-intensity", "agent-id", AGENT,
                "drive-type", type, "intensity", String.valueOf(intensity),
                "description", description)), TENANT);
    }

    private void seedGenerated(String driveType, String description) {
        store.addNode(NodeInput.of("drive-description-" + driveType, sgId)
            .withTraits(Set.of("DriveDescriptionGenerated"))
            .withProperties(Map.of("agent-id", AGENT, "drive-type", driveType,
                "generated-description", description)), TENANT);
    }

    private void seedProfile(String driveType, String inferredInputs) {
        store.addNode(NodeInput.of("drive-profile-" + driveType, sgId)
            .withTraits(Set.of("DriveProfileInferred"))
            .withProperties(Map.of("agent-id", AGENT, "drive-type", driveType,
                "inferred-inputs", inferredInputs)), TENANT);
    }

    private void seedCompetition(String driveA, String driveB, String diffText) {
        store.addNode(NodeInput.of("competition-" + driveA + "-" + driveB, sgId)
            .withTraits(Set.of("DriveCompetitionInferred"))
            .withProperties(Map.of("agent-id", AGENT,
                "drive-a", driveA, "drive-b", driveB,
                "differentiation-text", diffText)), TENANT);
    }

    private void seedFormativeMemory(String text, String driveType) {
        store.addNode(NodeInput.of(text, sgId)
            .withProperties(Map.of("cognitiveKind", "formative-experience", "agent-id", AGENT,
                "situation-types", "formation," + driveType)), TENANT);
    }

    private String renderWhatPulls() {
        var section = renderInternalState(List.of(), "");
        assertThat(section).isNotNull();
        return ((ObservationSection.TextBlock) section).content();
    }

    private ObservationSection renderInternalState(List<String> activated, String situation) {
        return renderInternalStateWithClassifier(activated, situation, null);
    }

    private ObservationSection renderInternalStateWithClassifier(
            List<String> activated, String situation,
            io.casehub.neocortex.caps.SituationClassifier classifier) {
        var configs = ManorSocialConfigLoader.loadForProfile("generic");
        var config = configs.getOrDefault(AGENT, SocialConfig.empty());
        var cognition = new CharacterCognition(AGENT, null, null, config, List.of(),
            null, new ManorContextStrategy(), null, null,
            TENANT, store, null, classifier);
        return cognition.renderInternalState(activated, situation != null ? situation : "");
    }
}
