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
 * scenario event text with the real CapsTopology.
 *
 * Finding: the classifier categorises situations from the SUBJECT's experiential
 * perspective, not from an OBSERVER's appraisal perspective. "Clara struggling
 * with a door" activates powerlessness/failure (Clara's experience), not
 * acceptance/sharing (PP's gallantry opportunity). This has implications for
 * how DriveProfileInferred inputs should be structured.
 */
@QuarkusTest
@Tag("llm-eval")
class SituationClassifierIntegrationTest {

    private static final Path OUTPUT_DIR = Path.of("target/experiment-results");

    // Social warmth — maps to relationship/social topology nodes
    private static final String SOCIAL_WARMTH_SITUATION =
            "Clara Bellingham welcomes you warmly, her smile genuine and open. " +
            "'I'm so glad you're here, James. It wouldn't feel the same without you.' " +
            "She links her arm through yours as you walk toward the library.";

    // Physical struggle — maps to threat/autonomy topology nodes (subject's experience)
    private static final String PHYSICAL_STRUGGLE_SITUATION =
            "Clara Bellingham is struggling with the heavy oak door to the library. " +
            "The hinges are rusted and the door won't budge. She's straining against " +
            "it, her notes slipping from under her arm.";

    // Competence/mastery — maps to achievement topology nodes
    private static final String COMPETENCE_SITUATION =
            "You have successfully repaired the mechanism. The gears mesh smoothly, " +
            "the lever clicks into place with satisfying precision. Every wire spliced, " +
            "every connection tested. The machine hums with renewed purpose.";

    // Investigation/puzzle — maps to achievement/autonomy nodes
    private static final String INVESTIGATION_SITUATION =
            "The inscription on the mantelpiece reads 'Cities but no houses, forests " +
            "but no trees.' A riddle. The library shelves hold references that might " +
            "decode it. You begin cataloguing the relevant volumes.";

    // Competition/rivalry — maps to resource/social nodes
    private static final String COMPETITION_SITUATION =
            "Foxworth is already at the treasure chest, his hands on the diamond. " +
            "He smirks at you. 'Too slow, Hartwell. This prize is mine.' The others " +
            "watch to see who will prevail in this contest.";

    // Neutral corridor — minimal activations expected
    private static final String NEUTRAL_SITUATION =
            "You walk down the empty corridor. Dust motes drift through the afternoon " +
            "light. The floorboards creak beneath your feet. Nothing remarkable — just " +
            "an old house settling.";

    // Real formation memory text — the exact input the pipeline classifies
    private static final String PP_GALLANTRY_MEMORY =
            "The girl from school was in trouble — bullied by older kids. " +
            "You intervened. You got beaten up. She bandaged your eye and said " +
            "'That was the bravest and stupidest thing I've ever seen.' " +
            "You felt like you'd won every race ever run.";

    // Drive description — what the drive IS, not how it formed
    private static final String PP_GALLANTRY_DESCRIPTION =
            "The instinct to be chivalrous, to show care through action and courtesy. " +
            "You don't just help — you help with style.";

    @Inject AgentProvider agentProvider;
    @Inject CapsEngine capsEngine;

    private LlmSituationClassifier classifier;

    @BeforeEach
    void setUp() throws Exception {
        classifier = new LlmSituationClassifier(agentProvider, capsEngine.topology());
        Files.createDirectories(OUTPUT_DIR);
    }

    @Test
    void socialWarmth_activatesRelationshipOrSocialNodes() {
        List<SituationActivation> activations = classifyWithRetry(SOCIAL_WARMTH_SITUATION);

        Set<String> nodeIds = activationIds(activations);
        writeResult("social-warmth", activations);

        assertThat(activations).isNotEmpty();
        assertThat(nodeIds).containsAnyOf(
                "acceptance", "sharing", "secure_attachment", "reward");
    }

    @Test
    void physicalStruggle_activatesThreatOrAutonomyNodes() {
        List<SituationActivation> activations = classifyWithRetry(PHYSICAL_STRUGGLE_SITUATION);

        Set<String> nodeIds = activationIds(activations);
        writeResult("physical-struggle", activations);

        assertThat(activations).isNotEmpty();
        assertThat(nodeIds).containsAnyOf(
                "powerlessness", "physical_threat", "failure", "controlled");
    }

    @Test
    void competenceSuccess_activatesAchievementNodes() {
        List<SituationActivation> activations = classifyWithRetry(COMPETENCE_SITUATION);

        Set<String> nodeIds = activationIds(activations);
        writeResult("competence-success", activations);

        assertThat(activations).isNotEmpty();
        assertThat(nodeIds).containsAnyOf(
                "mastery", "competence_recognition", "reward", "agency_granted");
    }

    @Test
    void investigation_activatesAchievementOrAutonomyNodes() {
        List<SituationActivation> activations = classifyWithRetry(INVESTIGATION_SITUATION);

        Set<String> nodeIds = activationIds(activations);
        writeResult("investigation", activations);

        assertThat(activations).isNotEmpty();
        assertThat(nodeIds).containsAnyOf(
                "mastery", "agency_granted", "choice_available", "competence_recognition");
    }

    @Test
    void competition_activatesCompetitionOrSocialNodes() {
        List<SituationActivation> activations = classifyWithRetry(COMPETITION_SITUATION);

        Set<String> nodeIds = activationIds(activations);
        writeResult("competition", activations);

        assertThat(activations).isNotEmpty();
        assertThat(nodeIds).containsAnyOf(
                "competition", "dominance", "social_threat", "failure");
    }

    @Test
    void neutralSituation_minimalActivations() {
        List<SituationActivation> activations = classifier.classify(NEUTRAL_SITUATION, Map.of());

        writeResult("neutral", activations);

        assertThat(activations.size())
                .as("Neutral corridor should produce few or no activations")
                .isLessThanOrEqualTo(2);
    }

    @Test
    void gallantryMemory_classifierActivations() {
        List<SituationActivation> activations = classifyWithRetry(PP_GALLANTRY_MEMORY);

        Set<String> nodeIds = activationIds(activations);
        writeResult("gallantry-memory", activations);

        assertThat(activations).isNotEmpty();
        // The memory describes heroic intervention — what does the classifier see?
        // Document actual activations for design decisions about profile derivation
        System.out.println("Gallantry memory activations: " + activations);
    }

    @Test
    void gallantryDescription_activatesSocialNodes() {
        List<SituationActivation> activations = classifyWithRetry(PP_GALLANTRY_DESCRIPTION);

        Set<String> nodeIds = activationIds(activations);
        writeResult("gallantry-description", activations);

        assertThat(activations).isNotEmpty();
        assertThat(nodeIds).as("Drive description should activate social nodes")
                .containsAnyOf("acceptance", "sharing", "secure_attachment");
    }

    @Test
    void differentSituations_produceDifferentProfiles() {
        Set<String> warmthIds = activationIds(classifyWithRetry(SOCIAL_WARMTH_SITUATION));
        Set<String> competenceIds = activationIds(classifyWithRetry(COMPETENCE_SITUATION));
        Set<String> struggleIds = activationIds(classifyWithRetry(PHYSICAL_STRUGGLE_SITUATION));

        writeResult("differentiation", Map.of(
                "warmth", warmthIds.toString(),
                "competence", competenceIds.toString(),
                "struggle", struggleIds.toString()));

        assertThat(warmthIds).as("Warmth and competence should differ")
                .isNotEqualTo(competenceIds);
        assertThat(warmthIds).as("Warmth and struggle should differ")
                .isNotEqualTo(struggleIds);
        assertThat(competenceIds).as("Competence and struggle should differ")
                .isNotEqualTo(struggleIds);
    }

    /**
     * LLM classification is non-deterministic. Retry up to 3 times when empty
     * results might be due to low-confidence responses or transient failures.
     */
    private List<SituationActivation> classifyWithRetry(String situation) {
        for (int attempt = 0; attempt < 3; attempt++) {
            List<SituationActivation> result = classifier.classify(situation, Map.of());
            if (!result.isEmpty()) return result;
            try { Thread.sleep(2000); } catch (InterruptedException e) { break; }
        }
        return classifier.classify(situation, Map.of());
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
