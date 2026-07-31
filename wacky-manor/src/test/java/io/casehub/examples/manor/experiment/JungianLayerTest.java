package io.casehub.examples.manor.experiment;

import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.examples.manor.ManorConstants;
import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.model.ProfileMode;
import io.casehub.platform.agent.AgentProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@QuarkusTest
@Tag("llm-eval")
@TestProfile(JungianLayerTest.Profile.class)
class JungianLayerTest {

    private static final ProfileMode PROFILE = ProfileMode.JUNGIAN;
    private static final int MAX_TURNS = 180;
    private static final Path OUTPUT_DIR = Path.of("target/experiment-results");

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("manor.scenario.profile", "JUNGIAN",
                    "manor.scenario.mode", "autonomous");
        }
    }

    @Inject AgentProvider agentProvider;
    @Inject AgentRegistry agentRegistry;
    @Inject SystemPromptRenderer renderer;

    @Test void run1() throws Exception { executeRun(1); }
    @Test void run2() throws Exception { executeRun(2); }
    @Test void run3() throws Exception { executeRun(3); }

    private void executeRun(int runNumber) throws Exception {
        var world = MansionLoader.loadWorld();
        var goalsByAgent = resolveGoals();
        String gitHash = resolveGitHash();

        var runner = new AutonomousScenarioRunner(agentProvider, resolveModelId(), gitHash);
        var result = runner.run(world, PROFILE, runNumber, goalsByAgent, MAX_TURNS, this::renderPrompt);

        var outputFile = OUTPUT_DIR.resolve(
                PROFILE.name().toLowerCase() + "-run-" + runNumber + ".json");
        TranscriptRecorder.writeJson(result, outputFile);

        System.out.printf("[%s run %d] verdict=%s turns=%d duration=%dms%n",
                PROFILE, runNumber, result.verdict(), result.totalTurns(), result.durationMs());
    }

    private Map<String, List<AgentGoal>> resolveGoals() {
        var goals = new HashMap<String, List<AgentGoal>>();
        for (String agentId : List.of("penelope-pitstop", "hooded-claw",
                "ant-hill-mob", "dick-dastardly", "peter-perfect")) {
            agentRegistry.findById(agentId, ManorConstants.TENANCY_ID)
                    .ifPresent(desc -> goals.put(agentId, desc.goals()));
        }
        return goals;
    }

    private String renderPrompt(String agentId) {
        var desc = agentRegistry.findById(agentId, ManorConstants.TENANCY_ID)
                .orElseThrow(() -> new IllegalArgumentException("No descriptor: " + agentId));
        return renderer.render(desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN)).content();
    }

    private String resolveModelId() {
        return agentProvider.getClass().getSimpleName();
    }

    private String resolveGitHash() {
        try {
            var process = Runtime.getRuntime()
                    .exec(new String[]{"git", "rev-parse", "--short", "HEAD"});
            return new String(process.getInputStream().readAllBytes()).trim();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
