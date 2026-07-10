package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.vocab.CasehubCapabilityTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class LearnedExclusionSubsumptionTest {

    @Inject AgentRegistry registry;
    @Inject CapabilityHealth capabilityHealth;
    @Inject BehavioralSignalStore behavioralSignalStore;
    @Inject VocabularyRegistry vocabularyRegistry;

    static AgentDescriptor securityReviewer(String tenancyId) {
        return AgentDescriptor.builder()
            .agentId("security-reviewer-learned")
            .tenancyId(tenancyId)
            .name("Security Code Reviewer")
            .slot("reviewer")
            .capabilities(List.of(
                AgentCapability.builder()
                    .name("security-code-review")
                    .capabilityVocabulary(CasehubCapabilityTerm.URI)
                    .build()))
            .disposition(AgentDisposition.builder().build())
            .build();
    }

    @Test
    void probe_subsumption_with_learned_exclusion_uses_declared_name() {
        var tenancyId = "tenant-learned-exclusion-sub";
        var agent = securityReviewer(tenancyId);
        registry.register(agent);

        // Resolve declared name using CapabilityResolver (as engine would)
        var resolved = CapabilityResolver.resolve(
            agent.capabilities(), "code-review", vocabularyRegistry);
        assertThat(resolved).isNotNull();
        assertThat(resolved.capability().name()).isEqualTo("security-code-review");

        // Record 3 DECLINE signals against the declared capability name
        for (int i = 0; i < 3; i++) {
            behavioralSignalStore.record(
                agent.agentId(), tenancyId,
                resolved.capability().name(), "rust",
                BehavioralSignal.DECLINE);
        }

        // Probe via subsumption query (code-review → matches security-code-review)
        var status = capabilityHealth.probe(
            agent, "code-review", ProbeContext.of("rust"));

        // Should be Excluded(LEARNED) — signals recorded under declared name are found
        assertThat(status).isInstanceOf(CapabilityStatus.Excluded.class);
        var excluded = (CapabilityStatus.Excluded) status;
        assertThat(excluded.source()).isEqualTo(CapabilityStatus.ExclusionSource.LEARNED);
        assertThat(excluded.declineCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void probe_exact_match_with_learned_exclusion_still_works() {
        var tenancyId = "tenant-learned-exclusion-exact";
        var agent = securityReviewer(tenancyId);
        registry.register(agent);

        // Record 3 DECLINEs directly against declared name
        for (int i = 0; i < 3; i++) {
            behavioralSignalStore.record(
                agent.agentId(), tenancyId,
                "security-code-review", "java",
                BehavioralSignal.DECLINE);
        }

        // Probe with exact name
        var status = capabilityHealth.probe(
            agent, "security-code-review", ProbeContext.of("java"));

        assertThat(status).isInstanceOf(CapabilityStatus.Excluded.class);
        assertThat(((CapabilityStatus.Excluded) status).source())
            .isEqualTo(CapabilityStatus.ExclusionSource.LEARNED);
    }
}
