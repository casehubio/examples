package io.casehub.examples.manor.experiment;

import io.casehub.examples.manor.agent.ManorCognitiveSeeder;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.examples.manor.agent.SocialConfig;
import io.casehub.neocortex.cognition.prompt.CognitionRenderContext;
import io.casehub.neocortex.memory.experience.ExperienceRecorder;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

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

    @Test
    void seedAndConsolidate_showsPipelineOutput() {
        var configs = ManorSocialConfigLoader.load();
        var seeder = new ManorCognitiveSeeder(mindMapStore);

        for (var entry : configs.entrySet()) {
            seeder.seed(entry.getKey(), entry.getValue(), TENANT);
            int memCount = seeder.seedFormationMemories(entry.getKey(), entry.getValue(), TENANT, experienceRecorder);
            System.out.printf("[%s] Seeded %d formation memories%n", entry.getKey(), memCount);
        }

        System.out.println("\n=== RUNNING CONSOLIDATION (sleep) ===\n");
        consolidationScheduler.consolidateNow(TENANT);
        System.out.println("\n=== CONSOLIDATION COMPLETE ===\n");

        var behavioral = new io.casehub.neocortex.cognition.prompt.BehavioralPromptSection(mindMapStore);

        for (String agentId : new String[]{"hooded-claw", "peter-perfect", "ant-hill-mob", "penelope-pitstop"}) {
            var ctx = new CognitionRenderContext(agentId, TENANT, null);
            var output = behavioral.render(ctx);
            System.out.printf("=== %s: BehavioralPromptSection ===%n", agentId);
            System.out.println(output != null ? output : "(no output — no attractors)");
            System.out.println();
        }

        // Also show what other CognitionCore sections produce
        var moodOrch = new io.casehub.neocortex.cognition.mood.MoodOrchestrator(
                io.casehub.neocortex.cognition.mood.MoodConfig.defaults());
        var moodSection = new io.casehub.neocortex.cognition.prompt.MoodPromptSection(moodOrch);

        var charDriveSection = new io.casehub.neocortex.cognition.prompt.CharacterDrivePromptSection(mindMapStore);

        for (String agentId : new String[]{"hooded-claw", "peter-perfect"}) {
            var ctx = new CognitionRenderContext(agentId, TENANT, null);

            var moodOutput = moodSection.render(ctx);
            System.out.printf("=== %s: MoodPromptSection ===%n", agentId);
            System.out.println(moodOutput != null ? moodOutput : "(no output)");

            var driveOutput = charDriveSection.render(ctx);
            System.out.printf("=== %s: CharacterDrivePromptSection ===%n", agentId);
            System.out.println(driveOutput != null ? driveOutput : "(no output)");
            System.out.println();
        }
    }
}
