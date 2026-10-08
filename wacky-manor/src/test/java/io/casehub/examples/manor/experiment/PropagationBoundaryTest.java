package io.casehub.examples.manor.experiment;

import io.casehub.examples.manor.agent.CharacterCognition;
import io.casehub.examples.manor.agent.ManorCognitiveSeeder;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.examples.manor.agent.SocialConfig;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.caps.CapsEngine;
import io.casehub.neocortex.cognition.prompt.BehavioralPromptSection;
import io.casehub.neocortex.cognition.prompt.CognitionRenderContext;
import io.casehub.neocortex.memory.experience.ExperienceRecorder;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.intelligence.consolidation.ConsolidationScheduler;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boundary tests for the personality propagation pipeline (#115).
 *
 * Each test targets one dead end from the root-to-tip analysis,
 * verifying the signal crosses the boundary between seeding and
 * processing/rendering. Tests that currently fail document the
 * gap; fixing the wiring makes them pass.
 */
@QuarkusTest
class PropagationBoundaryTest {

    @Inject MindMapStore mindMapStore;
    @Inject ExperienceRecorder experienceRecorder;
    @Inject ConsolidationScheduler consolidationScheduler;
    @Inject CapsEngine capsEngine;

    ManorCognitiveSeeder seeder;
    Map<String, SocialConfig> configs;

    @BeforeEach
    void setUp() {
        seeder = new ManorCognitiveSeeder(mindMapStore);
        configs = ManorSocialConfigLoader.loadForProfile("GENERIC");
    }

    /**
     * Dead end #5: drives seeded as MindMap nodes but no section surfaces them.
     *
     * Verifies that seed() creates drive-intensity nodes with the properties
     * that DriveAdaptationPhase expects: cognitiveKind, drive-type, intensity,
     * agent-id. If the shape matches, DAP can find and adapt them.
     */
    @Test
    void driveSeeded_nodesHavePropertiesDriveAdaptationPhaseExpects() {
        var tenant = "boundary-drives-" + System.nanoTime();
        var ppConfig = configs.getOrDefault("peter-perfect", SocialConfig.empty());

        seeder.seed("peter-perfect", ppConfig, tenant);

        var driveNodes = mindMapStore.listSubgraphs(tenant).stream()
                .flatMap(sg -> mindMapStore.nodesIn(sg.id(), tenant).stream())
                .filter(n -> "drive-intensity".equals(n.property("cognitiveKind").orElse(null)))
                .toList();

        assertFalse(driveNodes.isEmpty(),
                "seed() should create drive-intensity nodes in the MindMap");

        for (var node : driveNodes) {
            assertTrue(node.property("drive-type").isPresent(),
                    "drive node must have drive-type property");
            assertTrue(node.property("intensity").isPresent(),
                    "drive node must have intensity property");
            assertTrue(node.property("agent-id").isPresent(),
                    "drive node must have agent-id property");
            assertEquals("peter-perfect", node.property("agent-id").orElse(null));
        }

        var driveTypes = driveNodes.stream()
                .map(n -> (String) n.property("drive-type").orElse(null))
                .toList();
        assertTrue(driveTypes.stream().anyMatch(d -> "proving-worth".equals(d)),
                "PP should have a proving-worth drive node");
    }

    /**
     * Dead end #1: BehavioralPromptSection renders null because
     * behavioralEnabled = false in CognitionConfig.all().
     *
     * Seeds the full pipeline (drives + CAPS + formation memories),
     * runs consolidation to trigger BehavioralSynthesisPhase, then
     * calls BehavioralPromptSection.render() directly (bypassing
     * the CognitionConfig gate). If this fails, the consolidation
     * pipeline doesn't produce CAPS-generated behavioral attractors.
     */
    @Test
    void formationMemories_consolidation_behavioralSectionRenders() throws InterruptedException {
        var tenant = "wacky-manor";
        var agentId = "pp-boundary-behavioral";
        var ppConfig = configs.getOrDefault("peter-perfect", SocialConfig.empty());

        seeder.seed(agentId, ppConfig, tenant);
        seeder.seedCapsState(agentId, ppConfig, tenant, capsEngine);
        int memCount = seeder.seedFormationMemories(agentId, ppConfig, tenant, experienceRecorder);
        assertTrue(memCount > 0, "PP should have formation memories to seed");

        for (int i = 0; i < 5; i++) {
            Thread.sleep(200);
            consolidationScheduler.consolidateNow(tenant);
        }

        var behavioral = new BehavioralPromptSection(mindMapStore);
        var ctx = new CognitionRenderContext(agentId, tenant, null);
        var output = behavioral.render(ctx);

        assertNotNull(output,
                "BehavioralPromptSection should render non-null after seeding + consolidation");
    }

    /**
     * Dead end #3: seedCapsState() never called in production.
     *
     * Verifies that calling seedCapsState() initializes the CAPS
     * engine with PP's disposition axes, and that consolidation
     * produces behavioral nodes tagged for this agent.
     */
    @Test
    void capsState_producesAgentSpecificBehavioralNodes() throws InterruptedException {
        var tenant = "wacky-manor";
        var agentId = "pp-boundary-caps";
        var ppConfig = configs.getOrDefault("peter-perfect", SocialConfig.empty());

        seeder.seed(agentId, ppConfig, tenant);
        seeder.seedCapsState(agentId, ppConfig, tenant, capsEngine);
        seeder.seedFormationMemories(agentId, ppConfig, tenant, experienceRecorder);

        for (int i = 0; i < 5; i++) {
            Thread.sleep(200);
            consolidationScheduler.consolidateNow(tenant);
        }

        var behavioralNodes = mindMapStore.listSubgraphs(tenant).stream()
                .filter(sg -> "behavioral".equals(sg.type()))
                .flatMap(sg -> mindMapStore.nodesIn(sg.id(), tenant).stream())
                .filter(n -> n.traits().contains("CapsGenerated"))
                .filter(n -> agentId.equals(n.property("agent-id").orElse(null)))
                .toList();

        assertFalse(behavioralNodes.isEmpty(),
                "Consolidation after seedCapsState should produce CapsGenerated behavioral nodes");
    }

    /**
     * Dead end #2: renderFormationMemories() is private with zero callers.
     *
     * Verifies that renderCognitiveSections() includes a "Where You Come From"
     * section containing formation memories. This test SHOULD FAIL until the
     * wiring fix adds renderFormationMemories() to renderCognitiveSections().
     */
    @Test
    void renderCognitiveSections_includesFormationMemories() {
        var ppConfig = configs.getOrDefault("peter-perfect", SocialConfig.empty());
        assertFalse(ppConfig.formationMemories().isEmpty(),
                "PP should have formation memories in config");

        var cognition = new CharacterCognition("peter-perfect", null, null, ppConfig, List.of());
        var character = new CharacterState("peter-perfect", "peter-perfect", "Grand Hall", 0.0, List.of());
        var sections = cognition.renderCognitiveSections(character, List.of(), Map.of());

        var formationSection = sections.stream()
                .filter(s -> s.header().contains("Where You Come From"))
                .findFirst();

        assertTrue(formationSection.isPresent(),
                "renderCognitiveSections should include formation memories — dead end #2");
    }

    /**
     * Dead end #4: seedPeople() never called in production.
     *
     * Verifies that seedPeople() creates a social subgraph with overlay
     * nodes carrying per-observer relationship data. This is the substrate
     * that RelationshipStagePhase needs to evolve familiarity.
     */
    @Test
    void seedPeople_createsSocialSubgraphWithOverlays() {
        var tenant = "boundary-people-" + System.nanoTime();

        seeder.seedPeople(configs, tenant);

        var socialSubgraphs = mindMapStore.listSubgraphs(tenant).stream()
                .filter(sg -> "social".equals(sg.type()))
                .toList();

        assertFalse(socialSubgraphs.isEmpty(),
                "seedPeople should create a social-type subgraph");

        var allNodes = socialSubgraphs.stream()
                .flatMap(sg -> mindMapStore.nodesIn(sg.id(), tenant).stream())
                .toList();

        assertTrue(allNodes.size() > configs.size(),
                "social subgraph should have more nodes than agents (shared + overlays)");

        var entityNodes = allNodes.stream()
                .filter(n -> n.traits().contains("Entitylike"))
                .toList();

        assertFalse(entityNodes.isEmpty(),
                "seedPeople should create entity nodes for each character");

        assertTrue(allNodes.size() > entityNodes.size(),
                "social subgraph should have overlay nodes beyond shared entity nodes");
    }
}
