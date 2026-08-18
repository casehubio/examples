package io.casehub.examples.manor.agent;

import io.casehub.neocortex.memory.Memory;
import io.casehub.neocortex.memory.MemoryDomain;
import io.casehub.neocortex.memory.reflection.ReflectionEvent;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManorReflectionSynthesizerTest {

    @Test
    void synthesizesInsightsFromMemories() {
        String llmResponse = """
            [
              {"insight": "Sneekly is always near dangerous items", "importance": 0.8},
              {"insight": "Penelope trusts Sneekly too easily", "importance": 0.7}
            ]
            """;
        var synthesizer = new ManorReflectionSynthesizer(mockProvider(llmResponse));

        var memories = List.of(
            new Memory("m1", "agent-1", new MemoryDomain("manor"), "t1",
                null, "Sneekly picked up rat poison", Map.of(), Instant.now(), 0.8),
            new Memory("m2", "agent-1", new MemoryDomain("manor"), "t1",
                null, "Sneekly offered Penelope tea", Map.of(), Instant.now(), 0.6)
        );

        List<ReflectionEvent> results = synthesizer.synthesize("agent-1", "t1", memories, 1);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).insight()).isEqualTo("Sneekly is always near dangerous items");
        assertThat(results.get(0).importance()).isEqualTo(0.8);
        assertThat(results.get(0).agentId()).isEqualTo("agent-1");
        assertThat(results.get(0).tenantId()).isEqualTo("t1");
        assertThat(results.get(0).level()).isEqualTo(1);
        assertThat(results.get(1).insight()).isEqualTo("Penelope trusts Sneekly too easily");
    }

    @Test
    void returnsEmptyOnLlmFailure() {
        var synthesizer = new ManorReflectionSynthesizer(failingProvider());

        var memories = List.of(
            new Memory("m1", "agent-1", new MemoryDomain("manor"), "t1",
                null, "test memory", Map.of(), Instant.now(), 0.5)
        );

        List<ReflectionEvent> results = synthesizer.synthesize("agent-1", "t1", memories, 1);

        assertThat(results).isEmpty();
    }

    @Test
    void returnsEmptyForEmptySources() {
        var synthesizer = new ManorReflectionSynthesizer(mockProvider("[]"));

        List<ReflectionEvent> results = synthesizer.synthesize("agent-1", "t1", List.of(), 1);

        assertThat(results).isEmpty();
    }

    @Test
    void defaultsImportanceWhenNull() {
        String llmResponse = """
            [{"insight": "something interesting", "importance": null}]
            """;
        var synthesizer = new ManorReflectionSynthesizer(mockProvider(llmResponse));

        var memories = List.of(
            new Memory("m1", "a1", new MemoryDomain("manor"), "t1",
                null, "test", Map.of(), Instant.now(), 0.5)
        );

        List<ReflectionEvent> results = synthesizer.synthesize("a1", "t1", memories, 1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).importance()).isEqualTo(0.7);
    }

    private AgentProvider mockProvider(String responseText) {
        return new AgentProvider() {
            @Override
            public Multi<AgentEvent> invoke(io.casehub.platform.agent.AgentSessionConfig config) {
                return Multi.createFrom().item(new AgentEvent.TextDelta(responseText));
            }
            @Override
            public io.casehub.platform.agent.AgentSession openSession(io.casehub.platform.agent.AgentSessionInit init) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private AgentProvider failingProvider() {
        return new AgentProvider() {
            @Override
            public Multi<AgentEvent> invoke(io.casehub.platform.agent.AgentSessionConfig config) {
                return Multi.createFrom().failure(new RuntimeException("LLM unavailable"));
            }
            @Override
            public io.casehub.platform.agent.AgentSession openSession(io.casehub.platform.agent.AgentSessionInit init) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
