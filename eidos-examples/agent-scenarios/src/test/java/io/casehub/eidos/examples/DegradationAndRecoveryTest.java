package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DegradationAndRecoveryTest {

    @Inject AgentRegistry registry;
    @Inject AgentStateStore stateStore;
    @Inject CapabilityHealth health;

    static final String TENANCY = "default";
    AgentDescriptor descriptor;
    ProbeContext ctx;

    @BeforeEach
    void setUp() {
        stateStore.clear("worker-1", TENANCY);
        registry.register(AgentDescriptor.builder()
            .agentId("worker-1")
            .name("Data Worker")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7-sonnet")
            .slot("executor")
            .capabilities(List.of(AgentCapability.builder()
                .name("data-processing")
                .description("Processes and transforms structured data")
                .qualityHint(0.85)
                .latencyHintP50Ms(300L)
                .costHint("medium")
                .build()))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId(TENANCY)
            .build());
        descriptor = registry.findById("worker-1", TENANCY).orElseThrow();
        ctx = ProbeContext.of(null);
    }

    @Test
    void probe_returns_ready_when_no_degradation() {
        var status = health.probe(descriptor, "data-processing", ctx);
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void probe_returns_degraded_after_recording() {
        stateStore.record("worker-1", TENANCY, DegradationReason.RATE_LIMITED,
            Instant.now().plusSeconds(60));
        var status = health.probe(descriptor, "data-processing", ctx);
        assertThat(status).isInstanceOf(CapabilityStatus.Degraded.class);
        assertThat(((CapabilityStatus.Degraded) status).reason())
            .isEqualTo(DegradationReason.RATE_LIMITED);
    }

    @Test
    void probe_returns_ready_after_clear() {
        stateStore.record("worker-1", TENANCY, DegradationReason.RATE_LIMITED,
            Instant.now().plusSeconds(60));
        assertThat(health.probe(descriptor, "data-processing", ctx))
            .isInstanceOf(CapabilityStatus.Degraded.class);

        stateStore.clear("worker-1", TENANCY);
        assertThat(health.probe(descriptor, "data-processing", ctx))
            .isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void probe_returns_ready_after_ttl_expires() {
        stateStore.record("worker-1", TENANCY, DegradationReason.CONTEXT_EXHAUSTED,
            Instant.now().minusSeconds(1));
        var status = health.probe(descriptor, "data-processing", ctx);
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void degradation_reasons_are_distinguishable() {
        stateStore.record("worker-1", TENANCY, DegradationReason.OVERLOADED,
            Instant.now().plusSeconds(60));
        assertThat(((CapabilityStatus.Degraded) health.probe(descriptor, "data-processing", ctx))
            .reason()).isEqualTo(DegradationReason.OVERLOADED);

        stateStore.clear("worker-1", TENANCY);
        stateStore.record("worker-1", TENANCY, DegradationReason.DOMAIN_MISMATCH,
            Instant.now().plusSeconds(60));
        assertThat(((CapabilityStatus.Degraded) health.probe(descriptor, "data-processing", ctx))
            .reason()).isEqualTo(DegradationReason.DOMAIN_MISMATCH);
    }
}
