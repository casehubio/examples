package io.casehub.examples.manor.engine;

import io.casehub.blocks.agentic.social.drive.adaptation.DriveAdaptationPhase;
import io.casehub.blocks.agentic.social.drive.adaptation.DriveReinforcementEntry;
import io.casehub.blocks.agentic.social.drive.adaptation.DriveAdaptationConfig;
import io.casehub.blocks.agentic.social.drive.adaptation.ReinforcementDirection;
import io.casehub.blocks.agentic.social.drive.adaptation.RewardAxis;
import io.casehub.blocks.agentic.social.prompt.CharacterDrivePromptSection;
import io.casehub.blocks.speech.PromptContext;
import io.casehub.examples.manor.agent.ManorCognitiveSeeder;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DriveAdaptationIntegrationTest {

    record TestConfig(
        double learningRate, double arousalWeight,
        double minIntensity, double maxIntensity,
        int maxPerPass
    ) implements DriveAdaptationConfig {}

    @Test
    void fullLifecycle_seedAdaptRender() {
        var store = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var allConfigs = ManorSocialConfigLoader.load();
        var tenant = "integration-test";
        var agent = "hooded-claw";

        var seedResult = seeder.seed(agent, allConfigs.get(agent), tenant);
        assertNotNull(seedResult.subgraphId());

        store.addNode(NodeInput.of("conflict event 1", seedResult.subgraphId())
            .withProvenance("experience-consolidation")
            .withProperties(Map.of(
                "cognitiveKind", "experience",
                "agent-id", agent,
                "event-type", "conflict_resolution"))
            .withPleasure(0.7).withArousal(0.6),
            tenant);

        store.addNode(NodeInput.of("social event 1", seedResult.subgraphId())
            .withProvenance("experience-consolidation")
            .withProperties(Map.of(
                "cognitiveKind", "experience",
                "agent-id", agent,
                "event-type", "social_interaction"))
            .withPleasure(0.5).withArousal(0.3),
            tenant);

        var config = allConfigs.get(agent);
        var agentReinforcement = new HashMap<String, List<DriveReinforcementEntry>>();
        for (var entry : config.reinforcement().entrySet()) {
            agentReinforcement.put(entry.getKey(), entry.getValue().stream()
                .map(m -> new DriveReinforcementEntry(
                    m.drive(),
                    m.direction() != null ? ReinforcementDirection.valueOf(m.direction()) : null,
                    m.rewardAxis() != null ? RewardAxis.valueOf(m.rewardAxis()) : null))
                .toList());
        }

        var adaptConfig = new TestConfig(0.1, 0.3, 0.1, 1.0, 20);
        var phase = new DriveAdaptationPhase(
            store, Map.of(agent, agentReinforcement), adaptConfig);
        phase.run(tenant, List.of());

        var nodes = store.nodesIn(seedResult.subgraphId(), tenant);
        var schemingNode = nodes.stream()
            .filter(n -> "drive-intensity".equals(n.properties().get("cognitiveKind")))
            .filter(n -> "scheming".equals(n.properties().get("drive-type")))
            .findFirst().orElseThrow();

        double schemingIntensity = Double.parseDouble(
            schemingNode.properties().get("intensity"));
        assertNotEquals(0.9, schemingIntensity, 0.0001,
            "Scheming intensity should have changed from initial 0.9");

        var renderer = new CharacterDrivePromptSection(store);
        var rendered = renderer.contribute(new PromptContext(agent, tenant, null));
        assertNotNull(rendered);
        assertTrue(rendered.contains("scheming"), "Rendered output should contain scheming drive");
        assertTrue(rendered.contains("Character Motivations"), "Should have Character Motivations heading");
    }

    @Test
    void coldStartParity_seededValuesMatchYaml() {
        var store = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var allConfigs = ManorSocialConfigLoader.load();
        var tenant = "parity-test";
        var agent = "penelope-pitstop";

        var seedResult = seeder.seed(agent, allConfigs.get(agent), tenant);

        var nodes = store.nodesIn(seedResult.subgraphId(), tenant);
        var driveNodes = nodes.stream()
            .filter(n -> "drive-intensity".equals(n.properties().get("cognitiveKind")))
            .toList();

        assertEquals(3, driveNodes.size(), "Penelope has 3 drives");

        var curiosityNode = driveNodes.stream()
            .filter(n -> "curiosity".equals(n.properties().get("drive-type")))
            .findFirst().orElseThrow();
        assertEquals("0.7", curiosityNode.properties().get("intensity"));
        assertEquals("0.7", curiosityNode.properties().get("initial-intensity"));

        var harmonyNode = driveNodes.stream()
            .filter(n -> "social-harmony".equals(n.properties().get("drive-type")))
            .findFirst().orElseThrow();
        assertEquals("0.8", harmonyNode.properties().get("intensity"));
    }
}
