package io.casehub.examples.manor.experiment;

import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.examples.manor.ManorConstants;
import io.casehub.examples.manor.agent.AgentInvocationService;
import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.model.ProfileMode;
import io.casehub.platform.agent.AgentProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@Tag("llm-eval")
@TestProfile(ScaleTest.Profile.class)
class ScaleTest {

    static final List<String> SCALE_10_CHARACTERS = List.of(
        "penelope-pitstop", "hooded-claw", "ant-hill-mob",
        "dick-dastardly", "peter-perfect",
        "muttley", "pat-pending", "lazy-luke",
        "rock-slag", "rufus-ruffcut");

    private static final int MAX_TURNS = 200;
    private static final Path OUTPUT_DIR = Path.of("target/scale-results");

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "manor.scenario.profile", "COMPOSITE",
                "manor.scenario.mode", "autonomous",
                "manor.scenario.max-turns", "200");
        }
    }

    @Inject AgentProvider agentProvider;
    @Inject AgentRegistry agentRegistry;
    @Inject SystemPromptRenderer renderer;

    @Test
    void tenAgents200Turns() throws Exception {
        var invocationService = new AgentInvocationService(agentProvider, 60, 2, 2000);

        var runner = new AutonomousScenarioRunner(
            invocationService, null, resolveModelId(), resolveGitHash());

        var world = MansionLoader.loadWorld();
        Map<String, List<AgentGoal>> goals = SCALE_10_CHARACTERS.stream()
            .collect(Collectors.toMap(id -> id,
                id -> agentRegistry.findById(id, ManorConstants.TENANCY_ID)
                    .map(d -> d.goals()).orElse(List.of())));

        var result = runner.run(world, ProfileMode.COMPOSITE, 1, goals,
            MAX_TURNS, this::renderPrompt, SCALE_10_CHARACTERS);

        Files.createDirectories(OUTPUT_DIR);
        var report = ScaleReport.from(result, invocationService.metrics(),
            SCALE_10_CHARACTERS.size(), runner.lastTurnLatencies());
        System.out.println(report.summary());
        report.writeJson(OUTPUT_DIR.resolve("scale-10agents-200turns.json"));

        assertThat(result.verdict()).isNotNull();
        assertThat(result.totalTurns()).isGreaterThanOrEqualTo(1);
        assertThat(report.avgTurnLatencyMs()).as("avg turn latency < 30s")
            .isLessThan(30_000);
        assertThat(report.llmFallbacks()).as("no more than 10% fallbacks")
            .isLessThan(result.totalTurns());
    }

    private String renderPrompt(String agentId) {
        var desc = agentRegistry.findById(agentId, ManorConstants.TENANCY_ID).orElseThrow();
        var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        return renderer.render(desc, ctx).content();
    }

    private String resolveModelId() {
        try {
            return agentProvider.getClass().getSimpleName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String resolveGitHash() {
        try {
            return new String(Runtime.getRuntime()
                .exec(new String[]{"git", "rev-parse", "--short", "HEAD"})
                .getInputStream().readAllBytes()).strip();
        } catch (Exception e) {
            return "local";
        }
    }
}
