package io.casehub.eidos.examples.graph;

import io.casehub.eidos.examples.graph.InMemoryAgentGraph.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * V1 scenarios: structural data only, no enricher.
 * All tests must pass before Phase 3 infrastructure begins.
 */
class V1GraphScenarioTest {

    InMemoryAgentGraph graph;

    @BeforeEach
    void setUp() {
        graph = new InMemoryAgentGraph(); // NoOp enricher
    }

    @Test
    void v1_wilsonScoreForKnownValues() {
        // Agent A: n=20, all SUCCEEDED confidence 0.78 → Wilson ≈ 0.60
        List<OutcomeRecord> a = IntStream.range(0, 20)
            .mapToObj(i -> new OutcomeRecord("t" + i, TaskResult.SUCCEEDED, 0.78))
            .toList();
        double scoreA = InMemoryAgentGraph.wilsonScore(a);

        // Agent B: n=5, all SUCCEEDED confidence 0.90 → Wilson ≈ 0.53
        List<OutcomeRecord> b = IntStream.range(0, 5)
            .mapToObj(i -> new OutcomeRecord("t" + i, TaskResult.SUCCEEDED, 0.90))
            .toList();
        double scoreB = InMemoryAgentGraph.wilsonScore(b);

        assertThat(scoreA).isGreaterThan(scoreB);
        assertThat(scoreA).isBetween(0.55, 0.65); // ≈ 0.60
        assertThat(scoreB).isBetween(0.48, 0.58); // ≈ 0.53
    }

    @Test
    void v1_routingPrefersAgentWithBetterOutcomeHistory() {
        // Agent A: 20 tasks SUCCEEDED confidence 0.78
        for (int i = 0; i < 20; i++) {
            String tid = graph.recordTask("agent-a", "t1", "code-review", "rust", "ref-a-" + i);
            graph.recordOutcome(tid, TaskResult.SUCCEEDED, 0.78);
        }
        // Agent B: 5 tasks SUCCEEDED confidence 0.90
        for (int i = 0; i < 5; i++) {
            String tid = graph.recordTask("agent-b", "t1", "code-review", "rust", "ref-b-" + i);
            graph.recordOutcome(tid, TaskResult.SUCCEEDED, 0.90);
        }

        List<String> ranked = graph.topAgentsByOutcome("code-review", "rust", "t1", 10);

        // Wilson: agent-a ≈ 0.60, agent-b ≈ 0.53. Larger sample wins.
        assertThat(ranked.get(0)).isEqualTo("agent-a");
        assertThat(ranked).containsExactlyInAnyOrder("agent-a", "agent-b");
    }

    @Test
    void v1_historyQueryReturnsByCapabilityAndDomain() {
        String t1 = graph.recordTask("agent-x", "t1", "code-review", "java", "ref-1");
        String t2 = graph.recordTask("agent-x", "t1", "planning", "agile", "ref-2");
        graph.recordOutcome(t1, TaskResult.SUCCEEDED, 0.9);
        graph.recordOutcome(t2, TaskResult.SUCCEEDED, 0.8);

        AgentHistory history = graph.agentHistory("agent-x", "t1");

        assertThat(history.tasks()).hasSize(2);
        assertThat(history.outcomes()).hasSize(2);
        assertThat(history.tasks()).extracting(TaskRecord::capabilityTag)
            .containsExactlyInAnyOrder("code-review", "planning");
    }

    @Test
    void v1_attestationChainLinksOutcomeToLedgerHash() {
        String tid = graph.recordTask("agent-x", "t1", "code-review", "java", "ref-1");
        graph.recordOutcome(tid, TaskResult.SUCCEEDED, 0.9);
        graph.linkAttestation(tid, "agent-x", "t1", "hash-abc123", "MessageLedgerEntry");

        List<AttestationRecord> refs = graph.attestationsFor("agent-x", "t1");

        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).ledgerHash()).isEqualTo("hash-abc123");
        assertThat(refs.get(0).taskId()).isEqualTo(tid);
    }

    @Test
    void v1_insufficiencyWarningBelowThreshold() {
        for (int i = 0; i < 4; i++) {
            String tid = graph.recordTask("agent-x", "t1", "code-review", "java", "r" + i);
            graph.recordOutcome(tid, TaskResult.SUCCEEDED, 0.9);
        }

        AgentHistory history = graph.agentHistory("agent-x", "t1");

        assertThat(history.sufficiency().level()).isEqualTo(SufficiencyLevel.INSUFFICIENT);
        assertThat(history.sufficiency().sampleCount()).isEqualTo(4);
    }

    @Test
    void v1_indicativeAtFiveSamples() {
        for (int i = 0; i < 5; i++) {
            String tid = graph.recordTask("agent-x", "t1", "code-review", "java", "r" + i);
            graph.recordOutcome(tid, TaskResult.SUCCEEDED, 0.9);
        }
        assertThat(graph.agentHistory("agent-x", "t1").sufficiency().level())
            .isEqualTo(SufficiencyLevel.INDICATIVE);
    }

    @Test
    void v1_sufficientAtTenSamples() {
        for (int i = 0; i < 10; i++) {
            String tid = graph.recordTask("agent-x", "t1", "code-review", "java", "r" + i);
            graph.recordOutcome(tid, TaskResult.SUCCEEDED, 0.9);
        }
        assertThat(graph.agentHistory("agent-x", "t1").sufficiency().level())
            .isEqualTo(SufficiencyLevel.SUFFICIENT);
    }

    @Test
    void v1_systemFunctionsNormallyWithNoHistory() {
        AgentHistory history = graph.agentHistory("agent-unknown", "t1");

        assertThat(history.tasks()).isEmpty();
        assertThat(history.outcomes()).isEmpty();
        assertThat(history.sufficiency().level()).isEqualTo(SufficiencyLevel.INSUFFICIENT);

        List<String> ranked = graph.topAgentsByOutcome("code-review", "java", "t1", 5);
        assertThat(ranked).isEmpty();
    }

    @Test
    void v1_tenancyIsolation() {
        String tid = graph.recordTask("agent-x", "tenant-A", "code-review", "java", "ref");
        graph.recordOutcome(tid, TaskResult.SUCCEEDED, 0.9);

        AgentHistory tenantB = graph.agentHistory("agent-x", "tenant-B");
        assertThat(tenantB.tasks()).isEmpty();
    }

    @Test
    void v1_noEnricherWarningPresent() {
        graph.recordTask("agent-x", "t1", "code-review", "security", "r1");

        AgentHistory history = graph.agentHistory("agent-x", "t1");

        assertThat(history.sufficiency().warnings())
            .anyMatch(w -> w.contains("No TaskSemanticEnricher"));
    }
}
