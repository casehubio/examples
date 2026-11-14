package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.vocab.CasehubCapabilityTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates task-domain-scoped behavioral signals: DECLINE signals accumulate
 * per (capabilityName, qualifier) where qualifier is the task domain string.
 * Probing with a matching taskDomain triggers learned exclusion; probing with
 * null taskDomain skips the check entirely. Both "security-code-review" and
 * "code-review" query tags resolve to the same declared capability via subsumption.
 *
 * <p>TTL: in production, signals expire after a configurable TTL (default 30 days).
 * This test uses clear() to demonstrate the reset path — real TTL expiry happens
 * automatically in the store implementation.
 */
@QuarkusTest
class LearnedSpecializationScenarioTest {

    @Inject AgentRegistry registry;
    @Inject CapabilityHealth capabilityHealth;
    @Inject BehavioralSignalStore signalStore;

    private AgentDescriptor agent(String tenancyId) {
        return AgentDescriptor.builder()
                .agentId("specialization-agent")
                .tenancyId(tenancyId)
                .name("Code Review Specialist")
                .slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .epistemicDomains(Map.of("java", 0.95))
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();
    }

    @Test
    void agent_starts_ready_for_security_domain() {
        var tenancy = "spec-baseline";
        var desc = agent(tenancy);
        registry.register(desc);

        var status = capabilityHealth.probe(desc, "security-code-review",
                ProbeContext.of("security"));

        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void decline_signals_below_threshold_still_ready() {
        var tenancy = "spec-below-threshold";
        var desc = agent(tenancy);
        registry.register(desc);

        signalStore.record(desc.agentId(), tenancy, "code-review",
                "security", BehavioralSignal.DECLINE);
        signalStore.record(desc.agentId(), tenancy, "code-review",
                "security", BehavioralSignal.DECLINE);

        var status = capabilityHealth.probe(desc, "security-code-review",
                ProbeContext.of("security"));

        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
        assertThat(signalStore.count(desc.agentId(), tenancy, "code-review",
                "security", BehavioralSignal.DECLINE)).isEqualTo(2);
    }

    @Test
    void third_decline_triggers_learned_exclusion() {
        var tenancy = "spec-exclusion";
        var desc = agent(tenancy);
        registry.register(desc);

        for (int i = 0; i < 3; i++) {
            signalStore.record(desc.agentId(), tenancy, "code-review",
                    "security", BehavioralSignal.DECLINE);
        }

        var status = capabilityHealth.probe(desc, "security-code-review",
                ProbeContext.of("security"));

        assertThat(status).isInstanceOf(CapabilityStatus.Excluded.class);
        var excluded = (CapabilityStatus.Excluded) status;
        assertThat(excluded.source()).isEqualTo(CapabilityStatus.ExclusionSource.LEARNED);
        assertThat(excluded.domain()).isEqualTo("security");
        assertThat(excluded.declineCount()).isEqualTo(3);
    }

    @Test
    void null_task_domain_skips_learned_exclusion() {
        var tenancy = "spec-null-domain";
        var desc = agent(tenancy);
        registry.register(desc);

        for (int i = 0; i < 3; i++) {
            signalStore.record(desc.agentId(), tenancy, "code-review",
                    "security", BehavioralSignal.DECLINE);
        }

        var status = capabilityHealth.probe(desc, "code-review",
                ProbeContext.of(null));

        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void success_signals_recorded_on_core_capability() {
        var tenancy = "spec-success";
        var desc = agent(tenancy);
        registry.register(desc);

        signalStore.record(desc.agentId(), tenancy, "code-review",
                "java", BehavioralSignal.SUCCESS);
        signalStore.record(desc.agentId(), tenancy, "code-review",
                "java", BehavioralSignal.SUCCESS);

        assertThat(signalStore.count(desc.agentId(), tenancy, "code-review",
                "java", BehavioralSignal.SUCCESS)).isEqualTo(2);

        var learned = signalStore.learned(desc.agentId(), tenancy,
                "code-review", BehavioralSignal.SUCCESS);
        assertThat(learned).containsEntry("java", 2);
    }

    @Test
    void clear_resets_learned_exclusion() {
        var tenancy = "spec-clear";
        var desc = agent(tenancy);
        registry.register(desc);

        for (int i = 0; i < 3; i++) {
            signalStore.record(desc.agentId(), tenancy, "code-review",
                    "security", BehavioralSignal.DECLINE);
        }

        var excluded = capabilityHealth.probe(desc, "security-code-review",
                ProbeContext.of("security"));
        assertThat(excluded).isInstanceOf(CapabilityStatus.Excluded.class);

        signalStore.clear(desc.agentId(), tenancy, "code-review",
                BehavioralSignal.DECLINE);

        var ready = capabilityHealth.probe(desc, "security-code-review",
                ProbeContext.of("security"));
        assertThat(ready).isInstanceOf(CapabilityStatus.Ready.class);
    }
}
