package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentQuery;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.MatchDegree;
import io.casehub.eidos.api.RuntimeCollaborationQuery;
import io.casehub.eidos.vocab.CasehubCapabilityTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ProximityDiscoveryScenarioTest {

    static final String TENANCY = "proximity-test";

    @Inject AgentRegistry registry;
    @Inject RuntimeCollaborationQuery collaborationQuery;

    static AgentDescriptor agent(String id, String capName) {
        return AgentDescriptor.builder()
            .agentId(id).tenancyId(TENANCY).name("Agent " + id).slot("worker")
            .capabilities(List.of(
                AgentCapability.builder()
                    .name(capName)
                    .capabilityVocabulary(CasehubCapabilityTerm.URI)
                    .build()))
            .disposition(AgentDisposition.builder().build())
            .build();
    }

    @Test
    void proximity_finds_capability_neighbors_and_excludes_exact() {
        registry.register(agent("prox-agent-exact", "code-review"));
        registry.register(agent("prox-agent-neighbor", "security-code-review"));

        var result = registry.find(AgentQuery.byProximity("code-review", 2, TENANCY));

        assertThat(result).extracting(m -> m.descriptor().agentId())
            .contains("prox-agent-neighbor")
            .doesNotContain("prox-agent-exact");
    }

    @Test
    void proximity_results_carry_match_degree() {
        registry.register(agent("prox-degree-agent", "security-code-review"));

        var result = registry.find(AgentQuery.byProximity("code-review", 2, TENANCY));
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).resolvedCapability()).isNotNull();
        assertThat(result.get(0).resolvedCapability().degree())
            .isNotInstanceOf(MatchDegree.Exact.class)
            .isNotInstanceOf(MatchDegree.None.class);
    }

    @Test
    void collaboration_query_returns_empty_with_noop() {
        var result = collaborationQuery.collaborators("any-agent", TENANCY);
        assertThat(result).isEmpty();
    }
}
