package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class TenancyIsolationTest {

    @Inject AgentRegistry registry;

    @BeforeEach
    void registerAgentsInDifferentTenants() {
        registry.register(AgentDescriptor.builder()
            .agentId("tenant-a-agent")
            .name("Agent A")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(AgentCapability.builder().name("code-review").qualityHint(0.9).build()))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("tenant-a")
            .build());

        registry.register(AgentDescriptor.builder()
            .agentId("tenant-b-agent")
            .name("Agent B")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(AgentCapability.builder().name("code-review").qualityHint(0.9).build()))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("tenant-b")
            .build());
    }

    @Test
    void tenant_a_sees_only_own_agents() {
        var agents = registry.find(AgentQuery.all("tenant-a"));
        assertThat(agents.stream().map(m -> m.descriptor().agentId()).toList())
            .contains("tenant-a-agent")
            .doesNotContain("tenant-b-agent");
    }

    @Test
    void tenant_b_sees_only_own_agents() {
        var agents = registry.find(AgentQuery.all("tenant-b"));
        assertThat(agents.stream().map(m -> m.descriptor().agentId()).toList())
            .contains("tenant-b-agent")
            .doesNotContain("tenant-a-agent");
    }

    @Test
    void find_by_id_respects_tenancy() {
        assertThat(registry.findById("tenant-a-agent", "tenant-a")).isPresent();
        assertThat(registry.findById("tenant-a-agent", "tenant-b")).isEmpty();
    }

    @Test
    void nonexistent_tenant_returns_empty() {
        assertThat(registry.find(AgentQuery.all("tenant-c"))).isEmpty();
    }
}
