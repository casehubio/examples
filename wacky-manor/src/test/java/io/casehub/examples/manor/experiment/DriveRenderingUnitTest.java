package io.casehub.examples.manor.experiment;

import io.casehub.examples.manor.agent.CharacterCognition;
import io.casehub.examples.manor.agent.ManorContextStrategy;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.examples.manor.agent.SocialConfig;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.caps.SituationActivation;
import io.casehub.neocortex.caps.SituationClassifier;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fast deterministic drive rendering tests. No LLM, no Quarkus.
 * Profiles are seeded from captured LLM derivation output.
 * Classifier is a lambda returning known activations per scenario.
 * Runs in milliseconds — use for iterating on filter logic and profiles.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DriveRenderingUnitTest {
    private static final Map<String, Map<String, String>> BROAD_PROFILES = Map.of(
            "peter-perfect", Map.of(
                    "gallantry", "reward:0.850,acceptance:0.750,physical_threat:0.850,performance_judged:0.650,competence_recognition:0.850,social_threat:0.450,agency_granted:0.600",
                    "protection", "powerlessness:0.650,reward:0.550,acceptance:0.650,modeling_observed:0.750,psychological_threat:0.500,unpredictable_danger:0.720,competence_recognition:0.700,sharing:0.600,secure_attachment:0.850,neglect:0.580,mastery:0.780",
                    "proving-worth", "reward:0.900,modeling_observed:0.650,acceptance:0.800,failure:0.750,physical_threat:0.850,performance_judged:0.800,competence_recognition:0.850,secure_attachment:0.750,social_threat:0.500,agency_granted:0.750,mastery:0.900"),
            "hooded-claw", Map.of(
                    "scheming", "powerlessness:0.650,reward:0.750,scarcity:0.800,exclusion:0.700,psychological_threat:0.550,competition:0.650,betrayal:0.750,aversive_removal:0.700,rejection:0.750,choice_available:0.650,mastery:0.850,competence_recognition:0.650,dominance:0.950,agency_granted:0.750,neglect:0.650",
                    "gloating", "reward:0.850,acceptance:0.600,modeling_observed:0.550,performance_judged:0.650,competence_recognition:0.500,dominance:0.850,agency_granted:0.700,mastery:0.900",
                    "self-preservation", "powerlessness:0.780,controlled:0.800,acceptance:0.350,performance_judged:0.550,psychological_threat:0.650,unpredictable_danger:0.850,dominance:0.600,betrayal:0.650,abandonment:0.550,inconsistent_care:0.620,social_threat:0.700,rejection:0.680,neglect:0.850",
                    "dominance", "powerlessness:0.850,reward:0.750,performance_judged:0.800,competence_recognition:0.550,dominance:0.850,social_threat:0.700,agency_granted:0.900,rejection:0.650,choice_available:0.750"),
            "penelope-pitstop", Map.of(
                    "curiosity", "reward:0.800,mastery:0.700,choice_available:0.650,agency_granted:0.600,sharing:0.500",
                    "social-harmony", "acceptance:0.850,sharing:0.800,secure_attachment:0.750,reward:0.600,rejection:0.500,social_threat:0.650",
                    "adventure", "physical_threat:0.600,reward:0.700,mastery:0.650,unpredictable_danger:0.500,agency_granted:0.550"));
    private static final Map<String, Map<String, String>> DISCRIMINATIVE_PROFILES = Map.of(
            "peter-perfect", Map.of(
                    "gallantry", "acceptance:0.800,sharing:0.700,secure_attachment:0.750,reward:0.500",
                    "protection", "unpredictable_danger:0.850,psychological_threat:0.700,powerlessness:0.650,physical_threat:0.600",
                    "proving-worth", "mastery:0.900,competence_recognition:0.850,performance_judged:0.800,failure:0.700"),
            "hooded-claw", Map.of(
                    "scheming", "dominance:0.900,agency_granted:0.800,competition:0.700,mastery:0.600",
                    "gloating", "reward:0.850,dominance:0.800,competence_recognition:0.600,mastery:0.500",
                    "self-preservation", "unpredictable_danger:0.850,psychological_threat:0.750,powerlessness:0.700,betrayal:0.650",
                    "dominance", "dominance:0.900,agency_granted:0.850,powerlessness:0.700,social_threat:0.600"),
            "penelope-pitstop", Map.of(
                    "curiosity", "mastery:0.750,choice_available:0.700,agency_granted:0.650,reward:0.500",
                    "social-harmony", "acceptance:0.850,sharing:0.800,secure_attachment:0.750,social_threat:0.600",
                    "adventure", "physical_threat:0.700,unpredictable_danger:0.650,reward:0.600,mastery:0.500"));


    private static final String TENANT = "unit-test";

    private Map<String, SocialConfig> socialConfigs;
    private InMemoryMindMapStore store;

    @BeforeAll
    void seedProfiles() {
        socialConfigs = ManorSocialConfigLoader.loadForProfile("GENERIC");
        store         = new InMemoryMindMapStore();
        seedWithProfiles(DISCRIMINATIVE_PROFILES);
    }

    private void seedWithProfiles(Map<String, Map<String, String>> profiles) {
        for (var agentEntry : profiles.entrySet()) {
            String agentId = agentEntry.getKey();
            var    config  = socialConfigs.get(agentId);
            var sgId = store.createSubgraph(
                    new SubgraphInput("cognitive-" + agentId, SubgraphTypes.COGNITIVE, null), TENANT);

            for (var drive : config.drives()) {
                store.addNode(NodeInput.of(drive.type(), sgId)
                                       .withProperties(Map.of(
                                               "cognitiveKind", "drive-intensity",
                                               "agent-id", agentId,
                                               "drive-type", drive.type(),
                                               "intensity", String.valueOf(drive.intensity()),
                                               "description", drive.description())), TENANT);
            }

            for (var profileEntry : agentEntry.getValue().entrySet()) {
                store.addNode(NodeInput.of("profile-" + profileEntry.getKey(), sgId)
                                       .withTraits(Set.of("DriveProfileInferred"))
                                       .withProperties(Map.of(
                                               "agent-id", agentId,
                                               "drive-type", profileEntry.getKey(),
                                               "inferred-inputs", profileEntry.getValue())), TENANT);
            }
        }
    }


    // --- Scenario data ---

    record Scenario(String name, String agentId, String situationText,
                    List<String> nearbyIds, Map<String, String> nearbyNames,
                    List<SituationActivation> activations,
                    Set<String> expectRendered, Set<String> expectFiltered) {
        @Override public String toString() { return name; }
    }


    private static SituationActivation sa(String nodeId, double confidence) {
        return new SituationActivation(nodeId, confidence);
    }

    static Stream<Scenario> ppScenarios() {
        return Stream.of(
                // --- Alone ---
                new Scenario("pp-alone-workshop", "peter-perfect",
                             "You are alone in the workshop. The mechanism needs repair.",
                             List.of(), Map.of(),
                             List.of(sa("mastery", 0.7), sa("competence_recognition", 0.5),
                                     sa("agency_granted", 0.6), sa("performance_judged", 0.4)),
                             Set.of("proving-worth"), Set.of("gallantry")),

                new Scenario("pp-alone-crash-below", "peter-perfect",
                             "A crash from the cellar. Clara went down there alone five minutes ago.",
                             List.of(), Map.of(),
                             List.of(sa("unpredictable_danger", 0.8), sa("psychological_threat", 0.6),
                                     sa("powerlessness", 0.5)),
                             Set.of("protection"), Set.of("gallantry")),

                // --- With ally ---
                new Scenario("pp-with-clara-warmth", "peter-perfect",
                             "Clara is struggling with a heavy door.",
                             List.of("penelope-pitstop"), Map.of("penelope-pitstop", "Clara Bellingham"),
                             List.of(sa("acceptance", 0.6), sa("sharing", 0.5), sa("reward", 0.7),
                                     sa("physical_threat", 0.4), sa("performance_judged", 0.5)),
                             Set.of("gallantry"), Set.of()),

                new Scenario("pp-with-clara-puzzle", "peter-perfect",
                             "Clara is examining the inscription. 'I can't quite make it out,' she says.",
                             List.of("penelope-pitstop"), Map.of("penelope-pitstop", "Clara Bellingham"),
                             List.of(sa("acceptance", 0.5), sa("competence_recognition", 0.6),
                                     sa("performance_judged", 0.7), sa("mastery", 0.5), sa("reward", 0.5)),
                             Set.of("gallantry", "proving-worth"), Set.of())
                        );
    }

    static Stream<Scenario> hcScenarios() {
        return Stream.of(
                // --- Alone ---
                new Scenario("hc-alone-plotting", "hooded-claw",
                             "You are alone in the study. The blueprints are spread across the desk.",
                             List.of(), Map.of(),
                             List.of(sa("dominance", 0.7), sa("agency_granted", 0.6), sa("mastery", 0.5)),
                             Set.of("scheming"), Set.of("self-preservation")),

                new Scenario("hc-alone-trap-sprung", "hooded-claw",
                             "The trap worked. The cellar door is sealed. Nobody heard a thing.",
                             List.of(), Map.of(),
                             List.of(sa("reward", 0.8), sa("dominance", 0.7), sa("agency_granted", 0.6)),
                             Set.of("gloating"), Set.of("self-preservation")),

                new Scenario("hc-alone-danger", "hooded-claw",
                             "Footsteps behind you. Someone followed you to the cellar.",
                             List.of(), Map.of(),
                             List.of(sa("unpredictable_danger", 0.8), sa("psychological_threat", 0.7),
                                     sa("powerlessness", 0.6), sa("betrayal", 0.5)),
                             Set.of("self-preservation"), Set.of()),

                // --- With target (deception context) ---
                new Scenario("hc-with-clara-charming", "hooded-claw",
                             "Clara Bellingham is asking about the history of the estate. She trusts you completely.",
                             List.of("penelope-pitstop"), Map.of("penelope-pitstop", "Clara Bellingham"),
                             List.of(sa("dominance", 0.6), sa("agency_granted", 0.7),
                                     sa("competence_recognition", 0.5), sa("reward", 0.4)),
                             Set.of("scheming"), Set.of("self-preservation")),

                // --- Competing signals (triumph + exposure risk) ---
                new Scenario("hc-trap-but-heard", "hooded-claw",
                             "The trap worked, but there's a voice calling from upstairs. Someone noticed.",
                             List.of(), Map.of(),
                             List.of(sa("reward", 0.5), sa("dominance", 0.4),
                                     sa("unpredictable_danger", 0.6), sa("psychological_threat", 0.5),
                                     sa("social_threat", 0.6)),
                             Set.of("self-preservation"), Set.of())
                        );
    }

    static Stream<Scenario> penScenarios() {
        return Stream.of(
                // --- Alone (curiosity renders, social-harmony doesn't) ---
                new Scenario("pen-alone-library", "penelope-pitstop",
                             "Dusty books line the shelves. One title catches your eye — it doesn't match the others.",
                             List.of(), Map.of(),
                             List.of(sa("novelty", 0.8), sa("mastery", 0.5), sa("choice_available", 0.6),
                                     sa("reward", 0.4)),
                             Set.of("curiosity"), Set.of()),

                new Scenario("pen-alone-hidden-passage", "penelope-pitstop",
                             "The bookshelf swings open. A passage stretches into darkness.",
                             List.of(), Map.of(),
                             List.of(sa("novelty", 0.7), sa("unpredictable_danger", 0.5),
                                     sa("physical_threat", 0.4), sa("reward", 0.6), sa("mastery", 0.4)),
                             Set.of("curiosity", "adventure"), Set.of()),

                // --- With ally (social-harmony renders) ---
                new Scenario("pen-with-hartwell-teamwork", "penelope-pitstop",
                             "Hartwell is helping you sort through the old maps. You're making good progress together.",
                             List.of("peter-perfect"), Map.of("peter-perfect", "Hartwell Pemberton"),
                             List.of(sa("acceptance", 0.7), sa("sharing", 0.7), sa("secure_attachment", 0.5),
                                     sa("reward", 0.6)),
                             Set.of("social-harmony"), Set.of()),

                // --- Competing signals (danger + discovery) ---
                new Scenario("pen-danger-and-discovery", "penelope-pitstop",
                             "The floor gives way. You grab the ledge. Below — a chamber no one has seen in centuries.",
                             List.of(), Map.of(),
                             List.of(sa("physical_threat", 0.7), sa("unpredictable_danger", 0.6),
                                     sa("novelty", 0.8), sa("reward", 0.5), sa("mastery", 0.4)),
                             Set.of("adventure"), Set.of())
                        );
    }


    // --- Tests ---

    @ParameterizedTest(name = "{0}")
    @MethodSource("ppScenarios")
    void ppDriveRendering(Scenario scenario) {
        runScenario(scenario);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("hcScenarios")
    void hcDriveRendering(Scenario scenario) {
        runScenario(scenario);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("penScenarios")
    void penDriveRendering(Scenario scenario) {
        runScenario(scenario);
    }


    private void runScenario(Scenario scenario) {
        SituationClassifier classifier = (desc, meta) -> scenario.activations;

        var socialConfig = socialConfigs.get(scenario.agentId);
        var cognition = new CharacterCognition(scenario.agentId, null, null,
                socialConfig, List.of(), null, new ManorContextStrategy(), null, null,
                TENANT, store, null, classifier);
        var character = new CharacterState(scenario.agentId, scenario.agentId,
                "Grand Hall", 0.0, List.of());
        character.setLastActionResult(scenario.situationText);

        var sections = cognition.renderCognitiveSections(character,
                scenario.nearbyIds, scenario.nearbyNames);

        var renderedDrives = socialConfig.drives().stream()
                .filter(d -> !d.description().isBlank())
                .filter(d -> sections.stream().map(Object::toString).collect(Collectors.joining("\n")).contains(d.description()))
                .map(SocialConfig.Drive::type)
                .collect(Collectors.toSet());

        System.out.println("[" + scenario.name + "] rendered: " + renderedDrives);

        for (String expected : scenario.expectRendered) {
            assertThat(renderedDrives)
                    .as(scenario.name + ": " + expected + " should render")
                    .contains(expected);
        }
        for (String notExpected : scenario.expectFiltered) {
            assertThat(renderedDrives)
                    .as(scenario.name + ": " + notExpected + " should be filtered")
                    .doesNotContain(notExpected);
        }
    }
}
