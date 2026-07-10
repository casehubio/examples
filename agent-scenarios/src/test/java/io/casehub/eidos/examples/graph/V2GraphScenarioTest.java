package io.casehub.eidos.examples.graph;

import io.casehub.eidos.examples.graph.InMemoryAgentGraph.*;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * V2 scenarios: same data as V1, but with a TaskSemanticEnricher.
 * v2_routingPicksDifferentAgentThanV1 is the critical validation test.
 * If it does not show genuine divergence, the enricher adds no value — scope back.
 */
class V2GraphScenarioTest {

    /** Maps security/safety-critical tasks to riskAppetite axis. Treats them as equivalent. */
    static class TestEnricher implements TaskSemanticEnricher {
        @Override
        public Set<String> dispositionAxes(String cap, String domain) {
            if ("code-review".equals(cap) && Set.of("security", "safety-critical").contains(domain)) {
                return Set.of("riskAppetite", "ruleFollowing");
            }
            return Set.of();
        }

        @Override
        public boolean semanticallyEquivalent(String a, String b) {
            Set<String> group = Set.of("security", "safety-critical");
            return group.contains(a) && group.contains(b) && !a.equals(b);
        }

        @Override
        public OptionalInt significance(String cap, String domain) {
            if ("security".equals(domain) || "safety-critical".equals(domain)) return OptionalInt.of(5);
            return OptionalInt.empty();
        }
    }

    @Test
    void v2_semanticEquivalenceWidensOutcomePool() {
        // agent-y has tasks ONLY in "safety-critical" — zero in "security".
        // Without enricher: querying "security" returns nothing for agent-y (invisible).
        // With enricher: "safety-critical" ≡ "security" → agent-y appears in routing.

        InMemoryAgentGraph v1 = new InMemoryAgentGraph();
        InMemoryAgentGraph v2 = new InMemoryAgentGraph(new TestEnricher());

        for (InMemoryAgentGraph g : List.of(v1, v2)) {
            // agent-x: 10 tasks in "security"
            for (int i = 0; i < 10; i++) {
                String tid = g.recordTask("agent-x", "t1", "code-review", "security", "s" + i);
                g.recordOutcome(tid, TaskResult.SUCCEEDED, 0.6);
            }
            // agent-y: 10 tasks in "safety-critical" ONLY — never recorded in "security"
            for (int i = 0; i < 10; i++) {
                String tid = g.recordTask("agent-y", "t1", "code-review", "safety-critical", "sc" + i);
                g.recordOutcome(tid, TaskResult.SUCCEEDED, 0.9);
            }
        }

        List<String> v1Ranked = v1.topAgentsByOutcome("code-review", "security", "t1", 10);
        List<String> v2Ranked = v2.topAgentsByOutcome("code-review", "security", "t1", 10);

        // V1: only agent-x is visible (it has security tasks); agent-y is invisible
        assertThat(v1Ranked).containsExactly("agent-x");
        assertThat(v1Ranked).doesNotContain("agent-y");

        // V2: agent-y appears (safety-critical ≡ security via enricher) and ranks first (0.9 > 0.6)
        assertThat(v2Ranked).contains("agent-y");
        assertThat(v2Ranked.get(0)).isEqualTo("agent-y");
    }

    @Test
    void v2_routingPicksDifferentAgentThanV1() {
        // Agent A: 20 tasks in "security", confidence 0.70  → Wilson ≈ 0.516
        // Agent B:  3 tasks in "security" + 7 in "safety-critical", confidence 0.95
        //
        // Without enricher: agent-b has only 3 "security" tasks → Wilson ≈ 0.476 → agent-a wins
        // With enricher: agent-b sees 10 combined tasks at 0.95 → Wilson ≈ 0.715 → agent-b wins

        InMemoryAgentGraph v1 = new InMemoryAgentGraph();
        InMemoryAgentGraph v2 = new InMemoryAgentGraph(new TestEnricher());

        for (InMemoryAgentGraph g : List.of(v1, v2)) {
            for (int i = 0; i < 20; i++) {
                String tid = g.recordTask("agent-a", "t1", "code-review", "security", "a" + i);
                g.recordOutcome(tid, TaskResult.SUCCEEDED, 0.70);
            }
            for (int i = 0; i < 3; i++) {
                String tid = g.recordTask("agent-b", "t1", "code-review", "security", "bs" + i);
                g.recordOutcome(tid, TaskResult.SUCCEEDED, 0.95);
            }
            for (int i = 0; i < 7; i++) {
                String tid = g.recordTask("agent-b", "t1", "code-review", "safety-critical", "bsc" + i);
                g.recordOutcome(tid, TaskResult.SUCCEEDED, 0.95);
            }
        }

        List<String> v1Ranked = v1.topAgentsByOutcome("code-review", "security", "t1", 10);
        List<String> v2Ranked = v2.topAgentsByOutcome("code-review", "security", "t1", 10);

        // V1: agent-a wins (20 samples at 0.70, Wilson≈0.516) over agent-b (3 samples, Wilson≈0.476)
        assertThat(v1Ranked.get(0)).isEqualTo("agent-a");

        // V2: agent-b wins (10 combined samples at 0.95, Wilson≈0.715 beats agent-a's 0.516)
        assertThat(v2Ranked.get(0)).isEqualTo("agent-b");

        // The routing decisions are genuinely different — enricher changed the outcome
        assertThat(v1Ranked.get(0)).isNotEqualTo(v2Ranked.get(0));
    }

    @Test
    void v2_enricherAxesAvailableForPersonalityMapping() {
        TestEnricher e = new TestEnricher();
        assertThat(e.dispositionAxes("code-review", "security"))
            .containsExactlyInAnyOrder("riskAppetite", "ruleFollowing");
        assertThat(e.dispositionAxes("code-review", "java")).isEmpty();
        assertThat(e.semanticallyEquivalent("security", "safety-critical")).isTrue();
        assertThat(e.semanticallyEquivalent("security", "java")).isFalse();
    }

    @Test
    void v2_absenceOfEnricherRecordedInWarnings() {
        InMemoryAgentGraph noEnricher = new InMemoryAgentGraph();
        noEnricher.recordTask("agent-x", "t1", "code-review", "security", "r1");

        AgentHistory history = noEnricher.agentHistory("agent-x", "t1");

        assertThat(history.sufficiency().warnings())
            .anyMatch(w -> w.contains("No TaskSemanticEnricher"));
    }
}
