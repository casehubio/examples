package io.casehub.examples.manor.experiment;

import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.model.CompletionReason;
import io.casehub.examples.manor.model.ProfileMode;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

class AutonomousScenarioRunnerTest {

    private static final String WAIT_JSON = """
            {"thinking":"waiting","dialogue":null,"aside":null,"action":{"type":"WAIT","target":null,"withItem":null}}""";

    private static final List<String> HC_SCRIPT = List.of(
            """
            {"thinking":"I need the poison","dialogue":null,"aside":null,"action":{"type":"MOVE","target":"kitchen","withItem":null}}""",
            """
            {"thinking":"The poison!","dialogue":null,"aside":"Nyah-ha-ha!","action":{"type":"TAKE","target":"poison","withItem":null}}""",
            """
            {"thinking":"To the ballroom","dialogue":null,"aside":null,"action":{"type":"MOVE","target":"ballroom","withItem":null}}""",
            """
            {"thinking":"Into the tea!","dialogue":"More tea?","aside":"Nyah-ha-ha-HA!","action":{"type":"USE","target":"tea-service","withItem":"rat-poison"}}"""
    );

    @Test
    void scripted_poison_scenario_terminates_with_poisoned() {
        var hcResponses = new ConcurrentLinkedQueue<>(HC_SCRIPT);

        AgentProvider stubProvider = new AgentProvider() {
            @Override public Multi<AgentEvent> invoke(AgentSessionConfig config) {
                String response;
                if (config.systemPrompt().contains("hooded-claw") && !hcResponses.isEmpty()) {
                    response = hcResponses.poll();
                } else {
                    response = WAIT_JSON;
                }
                return Multi.createFrom().item(new AgentEvent.TextDelta(response));
            }
            @Override public AgentSession openSession(AgentSessionInit init) { throw new UnsupportedOperationException(); }
        };

        var runner = new AutonomousScenarioRunner(stubProvider, "test-model", "test-hash");
        var world = MansionLoader.loadWorld();

        var result = runner.run(world, ProfileMode.BASELINE, 1,
                Map.of(), 60, agentId -> "You are " + agentId);

        assertThat(result.verdict()).isEqualTo(CompletionReason.POISONED);
        assertThat(result.totalTurns()).isLessThan(60);
        assertThat(result.events()).isNotEmpty();
        assertThat(result.events()).anyMatch(e ->
                "action".equals(e.type()) && "USE".equals(e.action()) &&
                "tea-service".equals(e.target()));
    }

    @Test
    void turn_limit_terminates_when_no_poison() {
        AgentProvider stubProvider = new AgentProvider() {
            @Override public Multi<AgentEvent> invoke(AgentSessionConfig config) {
                return Multi.createFrom().item(new AgentEvent.TextDelta(WAIT_JSON));
            }
            @Override public AgentSession openSession(AgentSessionInit init) { throw new UnsupportedOperationException(); }
        };

        var runner = new AutonomousScenarioRunner(stubProvider, "test-model", "test-hash");
        var world = MansionLoader.loadWorld();

        var result = runner.run(world, ProfileMode.BASELINE, 1,
                Map.of(), 10, agentId -> "You are " + agentId);

        assertThat(result.verdict()).isEqualTo(CompletionReason.TURN_LIMIT);
        assertThat(result.totalTurns()).isEqualTo(10);
    }

    @Test
    void malformed_llm_response_falls_back_to_idle() {
        AgentProvider stubProvider = new AgentProvider() {
            @Override public Multi<AgentEvent> invoke(AgentSessionConfig config) {
                return Multi.createFrom().item(new AgentEvent.TextDelta("NOT JSON AT ALL"));
            }
            @Override public AgentSession openSession(AgentSessionInit init) { throw new UnsupportedOperationException(); }
        };

        var runner = new AutonomousScenarioRunner(stubProvider, "test-model", "test-hash");
        var world = MansionLoader.loadWorld();

        var result = runner.run(world, ProfileMode.BASELINE, 1,
                Map.of(), 3, agentId -> "You are " + agentId);

        assertThat(result.verdict()).isEqualTo(CompletionReason.TURN_LIMIT);
    }
}
