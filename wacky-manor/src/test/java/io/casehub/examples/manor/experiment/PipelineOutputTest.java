package io.casehub.examples.manor.experiment;

import io.casehub.examples.manor.agent.ManorCognitiveSeeder;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.neocortex.cognition.prompt.CognitionRenderContext;
import io.casehub.neocortex.memory.experience.ExperienceRecorder;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Seeds formation memories, runs consolidation, and prints what
 * BehavioralPromptSection produces. Validates the formal pipeline
 * produces personality signal from formation memories.
 */
@QuarkusTest
@Tag("llm-eval")
class PipelineOutputTest {

    private static final String TENANT = "wacky-manor";

    @Inject MindMapStore mindMapStore;
    @Inject ExperienceRecorder experienceRecorder;
    @Inject io.casehub.neocortex.mindmap.intelligence.consolidation.ConsolidationScheduler consolidationScheduler;
    @Inject
            io.casehub.neocortex.memory.CaseMemoryStore                                    caseMemoryStore;
    @Inject io.casehub.neocortex.caps.CapsEngine capsEngine;



    @Test
    void seedAndConsolidate_showsPipelineOutput() {
        var configs = ManorSocialConfigLoader.load();
        var seeder  = new ManorCognitiveSeeder(mindMapStore);

        for (var entry : configs.entrySet()) {
            seeder.seed(entry.getKey(), entry.getValue(), TENANT);
            seeder.seedCapsState(entry.getKey(), entry.getValue(), TENANT, capsEngine);
            int memCount = seeder.seedFormationMemories(entry.getKey(), entry.getValue(), TENANT, experienceRecorder);
            if (memCount > 0) {System.out.printf("[%s] Seeded %d formation memories%n", entry.getKey(), memCount);}
        }

        System.out.println("\n=== MEMORY STORE SCAN ===");
        var scan = caseMemoryStore.scan(new io.casehub.neocortex.memory.MemoryScanRequest(
                TENANT, io.casehub.neocortex.memory.experience.ExperienceEvents.DOMAIN.name(),
                null, null, 50, null));
        System.out.printf("Memories in store (experience domain): %d%n", scan.size());
        for (var mem : scan.stream().limit(3).toList()) {
            String txt = mem.text() != null ? mem.text().substring(0, Math.min(50, mem.text().length())) : "null";
            System.out.printf("  [%s] %s... attrs=%s%n", mem.subject(), txt, mem.attributes());
        }

        System.out.println("\n=== MINDMAP SUBGRAPHS (before sleep) ===");
        for (var sg : mindMapStore.listSubgraphs(TENANT)) {
            var nodes = mindMapStore.nodesIn(sg.id(), TENANT);
            System.out.printf("  '%s' (%s): %d nodes%n", sg.name(), sg.type(), nodes.size());
        }

        System.out.println("\n=== RUNNING CONSOLIDATION ===");
        for (int attempt = 0; attempt < 5; attempt++) {
            try { Thread.sleep(200); } catch (InterruptedException e) { break; }
            consolidationScheduler.consolidateNow(TENANT);
        }
        System.out.println("=== DONE ===");

        System.out.println("\n=== MINDMAP SUBGRAPHS (after sleep) ===");
        for (var sg : mindMapStore.listSubgraphs(TENANT)) {
            var nodes = mindMapStore.nodesIn(sg.id(), TENANT);
            System.out.printf("  '%s' (%s): %d nodes%n", sg.name(), sg.type(), nodes.size());
            if ("behavioral".equals(sg.type()) || "cognitive".equals(sg.type())) {
                for (var n : nodes.stream().limit(5).toList()) {
                    System.out.printf("    %s traits=%s agent=%s%n", n.name(), n.traits(),
                                      n.property("agent-id").orElse("?"));
                }
            }
        }

        System.out.println("\n=== ALL BEHAVIORAL NODES ===");
        var allBehavioral = mindMapStore.search(
            io.casehub.neocortex.mindmap.MindMapQuery.of(TENANT, 200)
                .withType(io.casehub.neocortex.mindmap.SubgraphTypes.BEHAVIORAL));
        for (var n : allBehavioral) {
            System.out.printf("  %s agent=%s strength=%s caps=%s traits=%s%n",
                n.name(),
                n.property("agent-id").orElse("?"),
                n.property("strength").orElse("?"),
                n.property("caps-node-id").orElse("?"),
                n.traits());
        }

        var behavioral = new io.casehub.neocortex.cognition.prompt.BehavioralPromptSection(mindMapStore);
        for (String agentId : new String[]{"hooded-claw", "peter-perfect"}) {
            var debugNodes = mindMapStore.search(
                io.casehub.neocortex.mindmap.MindMapQuery.of(TENANT, 100)
                    .withType(io.casehub.neocortex.mindmap.SubgraphTypes.BEHAVIORAL))
                .stream()
                .filter(n -> n.traits().contains("CapsGenerated"))
                .filter(n -> agentId.equals(n.property("agent-id").orElse(null)))
                .filter(n -> n.property("strength").map(Double::parseDouble).orElse(0.0) > 0.1)
                .toList();
            System.out.printf("[debug] %s: %d behavioral nodes match query%n", agentId, debugNodes.size());
            var ctx    = new CognitionRenderContext(agentId, TENANT, null);
            var output = behavioral.render(ctx);
            System.out.printf("\n=== %s: BehavioralPromptSection ===%n", agentId);
            System.out.println(output != null ? output : "(no output — no attractors)");
        }
    }
}
