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


    @Test
    void seedAndConsolidate_showsPipelineOutput() {
        var configs = ManorSocialConfigLoader.load();
        var seeder  = new ManorCognitiveSeeder(mindMapStore);

        for (var entry : configs.entrySet()) {
            seeder.seed(entry.getKey(), entry.getValue(), TENANT);
            int memCount = seeder.seedFormationMemories(entry.getKey(), entry.getValue(), TENANT, experienceRecorder);
            if (memCount > 0) {System.out.printf("[%s] Seeded %d formation memories%n", entry.getKey(), memCount);}
        }

        System.out.println("\n=== MEMORY STORE SCAN ===");
        var scan = caseMemoryStore.scan(new io.casehub.neocortex.memory.MemoryScanRequest(
                TENANT, io.casehub.neocortex.memory.experience.ExperienceEvents.DOMAIN.name(),
                null, null, 50, null));
        System.out.printf("Memories in store (experience domain): %d%n", scan.size());
        for (var mem : scan.stream().limit(5).toList()) {
            String txt = mem.text() != null ? mem.text().substring(0, Math.min(60, mem.text().length())) : "null";
            System.out.printf("  [%s] %s...%n", mem.subject(), txt);
        }

        System.out.println("\n=== MINDMAP SUBGRAPHS (before sleep) ===");
        for (var sg : mindMapStore.listSubgraphs(TENANT)) {
            var nodes = mindMapStore.nodesIn(sg.id(), TENANT);
            System.out.printf("  '%s' (%s): %d nodes%n", sg.name(), sg.type(), nodes.size());
        }

        System.out.println("\n=== RUNNING CONSOLIDATION ===");
        consolidationScheduler.consolidateNow(TENANT);
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

        var behavioral = new io.casehub.neocortex.cognition.prompt.BehavioralPromptSection(mindMapStore);
        for (String agentId : new String[]{"hooded-claw", "peter-perfect"}) {
            var ctx    = new CognitionRenderContext(agentId, TENANT, null);
            var output = behavioral.render(ctx);
            System.out.printf("\n=== %s: BehavioralPromptSection ===%n", agentId);
            System.out.println(output != null ? output : "(no output — no attractors)");
        }
    }
}
