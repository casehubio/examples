package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.affordance.ObservationSection;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests validating Layer B (memory calibration) and Layer C (drive amplification)
 * wiring. No LLM calls — these verify config parsing, rendering, and data flow.
 */
class LayerBCWiringTest {

    private static Map<String, SocialConfig> genericConfigs;

    private static final Set<String> ALL_CHARACTERS = Set.of(
            "hooded-claw", "penelope-pitstop", "peter-perfect",
            "dick-dastardly", "ant-hill-mob");

    private static final Set<String> VALID_CATALOGUE_IDS = Set.of(
            "attachment-secure", "attachment-anxious", "attachment-dismissive",
            "attachment-fearful", "attachment-earned-security", "attachment-trust-erosion",
            "bandura-mastery-efficacy", "bandura-vicarious-fear",
            "bandura-observational-learning", "bandura-model-imitation",
            "bandura-efficacy-gate",
            "bisbas-approach-dominance", "bisbas-inhibition",
            "cbt-negative-self", "cbt-negative-other", "cbt-negative-world",
            "cbt-positive-self", "cbt-positive-other", "cbt-positive-world",
            "cbt-catastrophising", "cbt-overgeneralisation", "cbt-schema-rigidity",
            "cbt-sudden-gains",
            "operant-positive-reinforcement", "operant-punishment-avoidance",
            "operant-vr-extinction-resistance", "operant-learned-helplessness",
            "operant-extinction-recovery", "operant-negative-reinforcement",
            "operant-autonomy-reinforcement",
            "trauma-chronic-fight", "trauma-chronic-flight", "trauma-chronic-freeze",
            "trauma-chronic-fawn", "trauma-dissociation", "trauma-kindling",
            "trauma-window-narrowing", "trauma-hypervigilance");

    @BeforeAll
    static void loadConfigs() {
        genericConfigs = ManorSocialConfigLoader.loadForProfile("GENERIC");
    }

    // --- Layer B: Catalogue field ---

    @ParameterizedTest
    @ValueSource(strings = {"hooded-claw", "penelope-pitstop", "peter-perfect",
                            "dick-dastardly", "ant-hill-mob"})
    void everyFormationMemory_hasNonPlaceholderCatalogue(String characterId) {
        var config = genericConfigs.get(characterId);
        assertThat(config).as("Config for %s", characterId).isNotNull();
        assertThat(config.formationMemories()).isNotEmpty();

        for (var mem : config.formationMemories()) {
            assertThat(mem.catalogue())
                    .as("%s age %d catalogue", characterId, mem.age())
                    .isNotEqualTo("formation-memory")
                    .isNotBlank();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"hooded-claw", "penelope-pitstop", "peter-perfect",
                            "dick-dastardly", "ant-hill-mob"})
    void everyFormationMemory_catalogueExistsInCatalogue(String characterId) {
        var config = genericConfigs.get(characterId);
        for (var mem : config.formationMemories()) {
            assertThat(VALID_CATALOGUE_IDS)
                    .as("%s age %d catalogue '%s' must exist in experience-behaviour-catalogue",
                            characterId, mem.age(), mem.catalogue())
                    .contains(mem.catalogue());
        }
    }

    @Test
    void catalogueMappings_matchResearchDocSection12_5() {
        assertCatalogue("dick-dastardly", 6, "attachment-dismissive");
        assertCatalogue("dick-dastardly", 8, "cbt-negative-self");
        assertCatalogue("dick-dastardly", 10, "bandura-mastery-efficacy");
        assertCatalogue("dick-dastardly", 12, "operant-positive-reinforcement");
        assertCatalogue("dick-dastardly", 14, "cbt-negative-self");

        assertCatalogue("hooded-claw", 6, "attachment-dismissive");
        assertCatalogue("hooded-claw", 9, "cbt-negative-other");
        assertCatalogue("hooded-claw", 12, "bandura-mastery-efficacy");
        assertCatalogue("hooded-claw", 14, "cbt-negative-other");
        assertCatalogue("hooded-claw", 15, "attachment-trust-erosion");
        assertCatalogue("hooded-claw", 18, "operant-autonomy-reinforcement");

        assertCatalogue("peter-perfect", 5, "attachment-secure");
        assertCatalogue("peter-perfect", 8, "bandura-mastery-efficacy");
        assertCatalogue("peter-perfect", 12, "cbt-positive-other");
        assertCatalogue("peter-perfect", 14, "bandura-mastery-efficacy");
        assertCatalogue("peter-perfect", 16, "cbt-positive-self");

        assertCatalogue("penelope-pitstop", 5, "bandura-mastery-efficacy");
        assertCatalogue("penelope-pitstop", 7, "attachment-secure");
        assertCatalogue("penelope-pitstop", 9, "cbt-positive-other");
        assertCatalogue("penelope-pitstop", 11, "bandura-mastery-efficacy");
        assertCatalogue("penelope-pitstop", 13, "cbt-positive-self");

        assertCatalogue("ant-hill-mob", 8, "attachment-secure");
        assertCatalogue("ant-hill-mob", 10, "trauma-hypervigilance");
        assertCatalogue("ant-hill-mob", 12, "cbt-positive-self");
        assertCatalogue("ant-hill-mob", 14, "attachment-earned-security");
        assertCatalogue("ant-hill-mob", 15, "attachment-trust-erosion");
    }

    // --- Layer B: Substrate rendering ---

    @ParameterizedTest
    @ValueSource(strings = {"hooded-claw", "penelope-pitstop", "peter-perfect",
                            "dick-dastardly", "ant-hill-mob"})
    void everyCharacter_hasSubstrateField(String characterId) {
        var config = genericConfigs.get(characterId);
        assertThat(config.substrate())
                .as("%s substrate", characterId)
                .isNotNull()
                .isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {"hooded-claw", "penelope-pitstop", "peter-perfect",
                            "dick-dastardly", "ant-hill-mob"})
    void renderedSections_includeWhatShapedYou(String characterId) {
        var config = genericConfigs.get(characterId);
        var cognition = new CharacterCognition(characterId, null, null, config, List.of());
        var character = new CharacterState(characterId, characterId, "Room", 0.0, List.of());
        var sections = cognition.renderCognitiveSections(character, List.of(), Map.of());

        var headers = sections.stream().map(ObservationSection::header).toList();
        assertThat(headers).contains("What Shaped You");
    }

    @ParameterizedTest
    @ValueSource(strings = {"hooded-claw", "penelope-pitstop", "peter-perfect",
                            "dick-dastardly", "ant-hill-mob"})
    void whatShapedYou_appearsAfterWhereYouComeFrom(String characterId) {
        var config = genericConfigs.get(characterId);
        var cognition = new CharacterCognition(characterId, null, null, config, List.of());
        var character = new CharacterState(characterId, characterId, "Room", 0.0, List.of());
        var sections = cognition.renderCognitiveSections(character, List.of(), Map.of());

        var headers = sections.stream().map(ObservationSection::header).toList();
        int memoriesIdx = headers.indexOf("Where You Come From");
        int substrateIdx = headers.indexOf("What Shaped You");
        assertThat(memoriesIdx).as("Where You Come From must exist").isGreaterThanOrEqualTo(0);
        assertThat(substrateIdx).as("What Shaped You must exist").isGreaterThanOrEqualTo(0);
        assertThat(substrateIdx).as("What Shaped You must follow Where You Come From")
                .isGreaterThan(memoriesIdx);
    }

    @Test
    void substrates_areDifferentPerCharacter() {
        var substrates = ALL_CHARACTERS.stream()
                .map(id -> genericConfigs.get(id).substrate())
                .collect(Collectors.toSet());
        assertThat(substrates).as("Each character should have unique substrate")
                .hasSize(ALL_CHARACTERS.size());
    }

    // --- Layer B: Serves annotations enrichment ---

    @Test
    void formationMemories_haveAnnotations() {
        var ddConfig = genericConfigs.get("dick-dastardly");
        var age8 = ddConfig.formationMemories().stream()
                .filter(m -> m.age() == 8).findFirst().orElseThrow();
        assertThat(age8.annotations())
                .as("DD age 8 should have serves annotation for recognition|defining")
                .isNotEmpty();
        assertThat(age8.annotations().getFirst().drive()).isEqualTo("recognition");
        assertThat(age8.annotations().getFirst().degree()).isEqualTo("defining");
    }

    @Test
    void formationMemories_inlineAnnotationsParsed() {
        var ddConfig = genericConfigs.get("dick-dastardly");
        var age6 = ddConfig.formationMemories().stream()
                .filter(m -> m.age() == 6).findFirst().orElseThrow();
        var drives = age6.annotations().stream()
                .map(SocialConfig.FormationMemory.ConcernAnnotation::drive)
                .collect(Collectors.toSet());
        assertThat(drives).containsExactlyInAnyOrder("greed", "scheming");
    }

    // --- Layer B: Seeder enriched situationTypes ---

    @Test
    void seeder_enrichesSituationTypesWithDriveAnnotations() {
        var store = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var ddConfig = genericConfigs.get("dick-dastardly");

        var recorder = new CapturingExperienceRecorder();
        seeder.seedFormationMemories("dick-dastardly", ddConfig, "test-tenant", recorder);

        assertThat(recorder.recorded).hasSize(5);

        var age8Event = recorder.recorded.stream()
                .filter(e -> e.description().contains("spelling bee"))
                .findFirst().orElseThrow();
        assertThat(age8Event.catalogueEntryId()).isEqualTo("cbt-negative-self");
        assertThat(age8Event.situationTypes()).contains("recognition");
        assertThat(age8Event.situationTypes()).contains("formation");

        var age6Event = recorder.recorded.stream()
                .filter(e -> e.description().contains("youngest of four"))
                .findFirst().orElseThrow();
        assertThat(age6Event.catalogueEntryId()).isEqualTo("attachment-dismissive");
        assertThat(age6Event.situationTypes()).contains("greed", "scheming");
    }

    // --- Layer B: Key mediating substrate themes ---

    @Test
    void ddSubstrate_mentionsRecognitionGap() {
        var substrate = genericConfigs.get("dick-dastardly").substrate();
        assertThat(substrate).containsIgnoringCase("nobody sees");
    }

    @Test
    void hcSubstrate_mentionsDistrust() {
        var substrate = genericConfigs.get("hooded-claw").substrate();
        assertThat(substrate).containsIgnoringCase("trust");
    }

    @Test
    void ppSubstrate_mentionsWorthThroughAction() {
        var substrate = genericConfigs.get("peter-perfect").substrate();
        assertThat(substrate).containsIgnoringCase("worth");
    }

    @Test
    void penSubstrate_mentionsHarmony() {
        var substrate = genericConfigs.get("penelope-pitstop").substrate();
        assertThat(substrate).containsIgnoringCase("peace");
    }

    @Test
    void ahmSubstrate_mentionsLoyaltyAndSuspicion() {
        var substrate = genericConfigs.get("ant-hill-mob").substrate();
        assertThat(substrate).containsIgnoringCase("loyalty");
        assertThat(substrate).containsIgnoringCase("trust");
    }

    // --- Layer C: D1 drive mapping coverage ---

    @Test
    void everyDriveType_hasDOneMappingInBehavioralSynthesis() {
        var allDriveTypes = ALL_CHARACTERS.stream()
                .flatMap(id -> genericConfigs.get(id).drives().stream())
                .map(SocialConfig.Drive::type)
                .collect(Collectors.toSet());

        assertThat(allDriveTypes)
                .as("All drive types used by characters")
                .isNotEmpty();

        for (String driveType : allDriveTypes) {
            assertThat(D1_MAPPINGS)
                    .as("D1 mapping must exist for drive type '%s'", driveType)
                    .containsKey(driveType);
        }
    }

    @Test
    void d1Mappings_matchBehavioralSynthesisPhase() {
        assertThat(D1_MAPPINGS.get("gloating")).containsExactly("dominance", "BAS_activation");
        assertThat(D1_MAPPINGS.get("scheming")).containsExactly("dominance", "distrust", "self_direct");
        assertThat(D1_MAPPINGS.get("recognition")).containsExactly("competence_recognition", "self_worth", "BAS_activation");
        assertThat(D1_MAPPINGS.get("social-harmony")).containsExactly("other_safety", "acceptance", "proximity_seek");
        assertThat(D1_MAPPINGS.get("gallantry")).containsExactly("other_safety", "acceptance", "approach");
    }

    // --- Helpers ---

    private void assertCatalogue(String characterId, int age, String expectedCatalogue) {
        var config = genericConfigs.get(characterId);
        var mem = config.formationMemories().stream()
                .filter(m -> m.age() == age).findFirst();
        assertThat(mem)
                .as("%s age %d should exist", characterId, age)
                .isPresent();
        assertThat(mem.get().catalogue())
                .as("%s age %d catalogue", characterId, age)
                .isEqualTo(expectedCatalogue);
    }

    private static final Map<String, List<String>> D1_MAPPINGS = Map.ofEntries(
            Map.entry("gallantry",         List.of("other_safety", "acceptance", "approach")),
            Map.entry("proving-worth",     List.of("mastery", "competence_recognition", "self_efficacy")),
            Map.entry("protection",        List.of("other_safety", "physical_threat", "approach")),
            Map.entry("scheming",          List.of("dominance", "distrust", "self_direct")),
            Map.entry("gloating",          List.of("dominance", "BAS_activation")),
            Map.entry("self-preservation", List.of("threat_sensitivity", "escape_assessment", "FFFS_activation")),
            Map.entry("recognition",       List.of("competence_recognition", "self_worth", "BAS_activation")),
            Map.entry("curiosity",         List.of("explore", "BAS_activation")),
            Map.entry("social-harmony",    List.of("other_safety", "acceptance", "proximity_seek")),
            Map.entry("adventure",         List.of("explore", "BAS_activation", "approach")),
            Map.entry("greed",             List.of("BAS_activation", "approach", "persist")),
            Map.entry("loyalty",           List.of("other_reliability", "proximity_seek", "trust")),
            Map.entry("suspicion",         List.of("threat_sensitivity", "distrust", "BIS_activation")),
            Map.entry("dominance",         List.of("dominance", "self_direct", "assert"))
    );

    private static class CapturingExperienceRecorder
            implements io.casehub.neocortex.memory.experience.ExperienceRecorder {

        final java.util.ArrayList<io.casehub.neocortex.memory.experience.FormativeExperience> recorded =
                new java.util.ArrayList<>();

        @Override
        public String record(io.casehub.neocortex.memory.experience.ExperienceEvent event) {
            if (event instanceof io.casehub.neocortex.memory.experience.FormativeExperience fe) {
                recorded.add(fe);
            }
            return "test-id";
        }

        @Override
        public io.casehub.neocortex.memory.experience.ExperienceStoreResult recordAll(
                java.util.List<io.casehub.neocortex.memory.experience.ExperienceEvent> events) {
            for (var e : events) record(e);
            return io.casehub.neocortex.memory.experience.ExperienceStoreResult.empty();
        }
    }
}
