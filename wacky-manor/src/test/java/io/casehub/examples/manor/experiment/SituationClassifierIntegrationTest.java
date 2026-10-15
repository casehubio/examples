package io.casehub.examples.manor.experiment;

import io.casehub.neocortex.caps.CapsEngine;
import io.casehub.neocortex.caps.SituationActivation;
import io.casehub.neocortex.caps.llm.LlmSituationClassifier;
import io.casehub.platform.agent.AgentProvider;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test exercising the REAL LlmSituationClassifier against actual
 * scenario event text. Validates that LLM activations align with the CapsTopology
 * input nodes, producing correct overlap signals for appraisal-based drive filtering.
 */
@QuarkusTest
@Tag("llm-eval")
class SituationClassifierIntegrationTest {

    private static final Path OUTPUT_DIR = Path.of("target/experiment-results");

    // Actual scenario event text from transcripts — not handcrafted test strings
    private static final String GALLANTRY_SITUATION =
            "Clara Bellingham is struggling with the heavy oak door to the library. " +
            "The hinges are rusted and the door won't budge. She's straining against " +
            "it, her notes slipping from under her arm.";

    private static final String COMPETENCE_SITUATION =
            "You are alone in the workshop. The mechanism's gnawed wire is preventing " +
            "the full assembly from engaging. You can feel the resistance in the lever, " +
            "a partial circuit at best. Time to strip the insulation and splice the connection.";

    private static final String SCHEMING_SITUATION =
            "The guests are gathered in the foyer, trusting and unsuspecting. " +
            "You must find the diamond before any of them suspect your true identity. " +
            "The estate manager disguise is holding — for now. Position yourself near " +
            "the study door before Foxworth gets there first.";

    private static final String INVESTIGATION_SITUATION =
            "The inscription on the mantelpiece reads 'Cities but no houses, forests " +
            "but no trees.' A riddle. The library shelves hold references that might " +
            "decode it. You begin cataloguing the relevant volumes.";

    private static final String NEUTRAL_SOLO_SITUATION =
            "You walk down the empty corridor. Dust motes drift through the afternoon " +
            "light. The floorboards creak beneath your feet. Nothing remarkable — just " +
            "an old house settling.";

    @Inject AgentProvider agentProvider;
    @Inject CapsEngine capsEngine;

    private LlmSituationClassifier classifier;

    @BeforeEach
    void setUp() throws Exception {
        classifier = new LlmSituationClassifier(agentProvider, capsEngine.topology());
        Files.createDirectories(OUTPUT_DIR);
    }

    @Test
    void gallantryScenario_activatesSocialNodes() {
        List<SituationActivation> activations = classifier.classify(GALLANTRY_SITUATION, Map.of());

        Set<String> nodeIds = activationIds(activations);
        writeResult("gallantry-scenario", activations);

        assertThat(activations).isNotEmpty();
        assertThat(nodeIds).containsAnyOf("acceptance", "sharing", "secure_attachment");
        assertThat(nodeIds).doesNotContain("mastery", "failure");
    }

    @Test
    void competenceScenario_activatesAchievementNodes() {
        List<SituationActivation> activations = classifier.classify(COMPETENCE_SITUATION, Map.of());

        Set<String> nodeIds = activationIds(activations);
        writeResult("competence-scenario", activations);

        assertThat(activations).isNotEmpty();
        assertThat(nodeIds).containsAnyOf("mastery", "agency_granted", "competence_recognition");
    }

    @Test
    void schemingScenario_activatesDominanceOrCompetitionNodes() {
        List<SituationActivation> activations = classifier.classify(SCHEMING_SITUATION, Map.of());

        Set<String> nodeIds = activationIds(activations);
        writeResult("scheming-scenario", activations);

        assertThat(activations).isNotEmpty();
        assertThat(nodeIds).containsAnyOf("dominance", "competition", "betrayal", "psychological_threat");
    }

    @Test
    void investigationScenario_activatesAchievementNodes() {
        List<SituationActivation> activations = classifier.classify(INVESTIGATION_SITUATION, Map.of());

        Set<String> nodeIds = activationIds(activations);
        writeResult("investigation-scenario", activations);

        assertThat(activations).isNotEmpty();
        assertThat(nodeIds).containsAnyOf("mastery", "agency_granted", "competence_recognition");
    }

    @Test
    void neutralScenario_fewOrNoSocialActivations() {
        List<SituationActivation> activations = classifier.classify(NEUTRAL_SOLO_SITUATION, Map.of());

        Set<String> nodeIds = activationIds(activations);
        writeResult("neutral-scenario", activations);

        assertThat(nodeIds).doesNotContain("acceptance", "sharing", "secure_attachment");
    }

    @Test
    void gallantryVsCompetence_differentActivationProfiles() {
        List<SituationActivation> gallantryActs = classifier.classify(GALLANTRY_SITUATION, Map.of());
        List<SituationActivation> competenceActs = classifier.classify(COMPETENCE_SITUATION, Map.of());

        Set<String> gallantryIds = activationIds(gallantryActs);
        Set<String> competenceIds = activationIds(competenceActs);

        writeResult("gallantry-vs-competence", Map.of(
                "gallantry", gallantryActs.toString(),
                "competence", competenceActs.toString()));

        boolean gallantryHasSocial = gallantryIds.stream()
                .anyMatch(id -> Set.of("acceptance", "sharing", "secure_attachment").contains(id));
        boolean competenceHasMastery = competenceIds.stream()
                .anyMatch(id -> Set.of("mastery", "agency_granted", "competence_recognition").contains(id));

        assertThat(gallantryHasSocial)
                .as("Gallantry scenario should activate social nodes")
                .isTrue();
        assertThat(competenceHasMastery)
                .as("Competence scenario should activate achievement nodes")
                .isTrue();
    }

    private static Set<String> activationIds(List<SituationActivation> activations) {
        return activations.stream()
                .map(SituationActivation::nodeId)
                .collect(Collectors.toSet());
    }

    private void writeResult(String testName, Object data) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Path outFile = OUTPUT_DIR.resolve("situation-classifier-" + testName + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(outFile.toFile(),
                    Map.of("test", testName, "data", data));
        } catch (Exception e) {
            System.err.println("Failed to write result for " + testName + ": " + e.getMessage());
        }
    }
}
