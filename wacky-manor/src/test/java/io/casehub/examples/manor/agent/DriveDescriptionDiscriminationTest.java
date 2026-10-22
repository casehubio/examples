package io.casehub.examples.manor.agent;

import io.casehub.agency.perception.observation.affordance.ObservationSection;
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
 * Diagnostic tests for drive description discrimination quality.
 *
 * These tests DON'T verify correctness — they verify DISTINGUISHABILITY.
 * Two drive descriptions that use identical somatic vocabulary fail to
 * discriminate, even if each is individually correct.
 *
 * The root cause of collapsed drives (PP gallantry 4.50→2.93, HC gloating
 * 3.97→2.16): the "somatic only" generation prompt produces descriptions
 * that share vocabulary across competing drives. B0 hand-authored
 * descriptions discriminated through non-somatic channels (audience
 * awareness, result-orientation, attentional narrowing).
 */
class DriveDescriptionDiscriminationTest {

    private InMemoryMindMapStore store;
    private String sgId;
    private static final String TENANT = "discrimination-test";
    private static final String AGENT = "peter-perfect";

    @BeforeEach
    void setUp() {
        store = new InMemoryMindMapStore();
        sgId = store.createSubgraph(
            new SubgraphInput("cognitive-" + AGENT, SubgraphTypes.COGNITIVE, null), TENANT);
    }

    @Test
    void generatedDescriptions_mustNotShareKeyVocabulary() {
        String gallantry = "A hot wave floods upward through your chest and locks your shoulders back, "
            + "your ribcage expanding until it aches. Your hands tingle with a fierce warmth.";
        String provingWorth = "A dense, hot weight settles behind your sternum. "
            + "Your hands feel restless and warm, almost electric at the fingertips.";
        String protection = "A band of tension locks across your upper back and shoulders. "
            + "Your jaw sets and your breathing drops low into your belly.";

        // These are CURRENT generated descriptions (from the live run).
        // They should be distinguishable. Count shared somatic words.
        var gallantryWords = new java.util.HashSet<>(java.util.Arrays.asList(gallantry.toLowerCase().split("\\W+")));
        var provingWords = new java.util.HashSet<>(java.util.Arrays.asList(provingWorth.toLowerCase().split("\\W+")));
        var protectionWords = new java.util.HashSet<>(java.util.Arrays.asList(protection.toLowerCase().split("\\W+")));

        var somaticVocab = Set.of("chest", "warmth", "warm", "hot", "tension",
            "hands", "shoulders", "sternum", "jaw", "tingle", "aches", "heat");

        long gallantryShared = somaticVocab.stream()
            .filter(w -> gallantryWords.contains(w) && provingWords.contains(w))
            .count();

        System.out.println("Gallantry-ProvingWorth shared somatic words: " + gallantryShared);
        System.out.println("Shared words: " + somaticVocab.stream()
            .filter(w -> gallantryWords.contains(w) && provingWords.contains(w))
            .toList());

        // If more than 3 somatic words are shared, the descriptions don't discriminate
        assertThat(gallantryShared)
            .as("Gallantry and proving-worth should not share more than 3 somatic words — "
                + "they need different bodily signatures or non-somatic channels")
            .isLessThanOrEqualTo(3);
    }

    @Test
    void b0Descriptions_discriminateThroughNonSomaticChannels() {
        String b0Gallantry = "There's a warmth that rises in your chest when courtesy matters — "
            + "not from the helping itself, but from something in the way it happens. "
            + "A rush of pleasure when the gesture has shape to it, when it lands with weight. "
            + "If nobody notices, the warmth drains away — the feeling needs an audience.";
        String b0ProvingWorth = "There's a tension in your shoulders that doesn't release "
            + "until you see it work — not the gesture, the result. A hollow ache when someone "
            + "else gets there first, not jealousy but something unfinished.";

        // B0 gallantry contains audience-dependence (attentional channel)
        assertThat(b0Gallantry).contains("audience");
        assertThat(b0Gallantry).contains("the way it happens");

        // B0 proving-worth contains result-orientation (temporal channel)
        assertThat(b0ProvingWorth).contains("the result");
        assertThat(b0ProvingWorth).contains("doesn't release until");

        // These are non-somatic discrimination signals that the "somatic only" prompt strips
    }

    @Test
    void renderedObservation_gallantryAndProvingWorthMustDiffer() {
        seedDrive("gallantry", 0.9);
        seedDrive("proving-worth", 0.7);

        // Use descriptions that SHOULD discriminate
        seedGenerated("gallantry",
            "A warmth rises through your chest — but it's not about what your hands can do. "
            + "It's about what they say. The gesture itself is the feeling. "
            + "Without a witness, the warmth drains.");
        seedGenerated("proving-worth",
            "A tension gathers in your hands that doesn't release until you see the result. "
            + "The ache is in the gap between effort and evidence. "
            + "Stillness feels like falling behind.");

        var content = renderWhatPulls();

        // Gallantry description should contain audience/witness language
        assertThat(content).contains("witness");
        // Proving-worth should contain result/evidence language
        assertThat(content).contains("result");
        // Both should be present — not one crowding out the other
        assertThat(content).contains("gesture itself is the feeling");
        assertThat(content).contains("falling behind");
    }

    @Test
    void somaticOnlyDescriptions_failToDiscriminate() {
        seedDrive("gallantry", 0.9);
        seedDrive("proving-worth", 0.7);

        // These are somatic-only — they DON'T discriminate
        seedGenerated("gallantry",
            "A hot wave floods your chest and locks your shoulders back. "
            + "Your hands tingle with fierce warmth.");
        seedGenerated("proving-worth",
            "A dense hot weight settles behind your sternum. "
            + "Your hands feel restless and warm at the fingertips.");

        var content = renderWhatPulls();

        // Both descriptions use "hot" + "chest/sternum" + "hands" + "warm"
        // A human reader could not tell which is gallantry and which is proving-worth
        // This test documents the discrimination failure
        System.out.println("=== SOMATIC-ONLY OBSERVATION ===");
        System.out.println(content);
        System.out.println("=== Can you tell which is gallantry? ===");
    }

    // --- helpers ---

    private void seedDrive(String type, double intensity) {
        store.addNode(NodeInput.of(type, sgId)
            .withProperties(Map.of("cognitiveKind", "drive-intensity", "agent-id", AGENT,
                "drive-type", type, "intensity", String.valueOf(intensity),
                "description", "fallback")), TENANT);
    }

    private void seedGenerated(String driveType, String description) {
        store.addNode(NodeInput.of("drive-description-" + driveType, sgId)
            .withTraits(Set.of("DriveDescriptionGenerated"))
            .withProperties(Map.of("agent-id", AGENT, "drive-type", driveType,
                "generated-description", description)), TENANT);
    }

    private String renderWhatPulls() {
        var configs = ManorSocialConfigLoader.loadForProfile("generic");
        var config = configs.getOrDefault(AGENT, SocialConfig.empty());
        var cognition = new CharacterCognition(AGENT, null, null, config, List.of(),
            null, new ManorContextStrategy(), null, null,
            TENANT, store, null, null);
        var section = cognition.renderInternalState(List.of(), "");
        assertThat(section).isNotNull();
        return ((ObservationSection.TextBlock) section).content();
    }
}
