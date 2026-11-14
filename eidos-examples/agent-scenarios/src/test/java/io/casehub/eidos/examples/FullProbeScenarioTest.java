package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus.*;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.vocab.CasehubCapabilityTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Complete probe pipeline reference — every CapabilityStatus variant in one test class.
 * Each test method is self-contained with its own tenancy ID.
 *
 * <p>Probe check order: Degraded → Unavailable → Excluded(DECLARED) →
 * Excluded(LEARNED) → EpistemicallyWeak → BehavioralViolation → Ready.
 */
@QuarkusTest
class FullProbeScenarioTest {

    @Inject AgentRegistry registry;
    @Inject CapabilityHealth capabilityHealth;
    @Inject AgentStateStore stateStore;
    @Inject BehavioralSignalStore signalStore;

    private AgentDescriptor codeReviewAgent(String agentId, String tenancyId) {
        return AgentDescriptor.builder()
                .agentId(agentId).tenancyId(tenancyId)
                .name(agentId).slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();
    }

    @Test
    void degradation_overrides_everything() {
        var tenancy = "probe-degraded";
        var desc = codeReviewAgent("degraded-agent", tenancy);
        registry.register(desc);

        stateStore.record(desc.agentId(), tenancy,
                DegradationReason.RATE_LIMITED,
                Instant.now().plus(1, ChronoUnit.HOURS));

        var status = capabilityHealth.probe(desc, "code-review",
                ProbeContext.of("java"));

        assertThat(status).isInstanceOf(Degraded.class);
        assertThat(((Degraded) status).reason())
                .isEqualTo(DegradationReason.RATE_LIMITED);
    }

    @Test
    void undeclared_capability_returns_unavailable() {
        var tenancy = "probe-unavailable";
        var desc = codeReviewAgent("undeclared-agent", tenancy);
        registry.register(desc);

        var status = capabilityHealth.probe(desc, "testing",
                ProbeContext.of(null));

        assertThat(status).isInstanceOf(Unavailable.class);
    }

    @Test
    void declared_domain_exclusion() {
        var tenancy = "probe-declared-excl";
        var desc = AgentDescriptor.builder()
                .agentId("domain-excluded-agent").tenancyId(tenancy)
                .name("Domain Excluded Agent").slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .excludedDomains(Set.of("rust"))
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();
        registry.register(desc);

        var status = capabilityHealth.probe(desc, "code-review",
                ProbeContext.of("rust"));

        assertThat(status).isInstanceOf(Excluded.class);
        var excluded = (Excluded) status;
        assertThat(excluded.source()).isEqualTo(ExclusionSource.DECLARED);
        assertThat(excluded.domain()).isEqualTo("rust");
        assertThat(excluded.declineCount()).isZero();
    }

    @Test
    void learned_domain_exclusion() {
        var tenancy = "probe-learned-excl";
        var desc = codeReviewAgent("learned-excluded-agent", tenancy);
        registry.register(desc);

        for (int i = 0; i < 3; i++) {
            signalStore.record(desc.agentId(), tenancy, "code-review",
                    "rust", BehavioralSignal.DECLINE);
        }

        var status = capabilityHealth.probe(desc, "code-review",
                ProbeContext.of("rust"));

        assertThat(status).isInstanceOf(Excluded.class);
        var excluded = (Excluded) status;
        assertThat(excluded.source()).isEqualTo(ExclusionSource.LEARNED);
        assertThat(excluded.domain()).isEqualTo("rust");
        assertThat(excluded.declineCount()).isEqualTo(3);
    }

    @Test
    void epistemic_weakness_below_confidence() {
        var tenancy = "probe-weak";
        var desc = AgentDescriptor.builder()
                .agentId("weak-agent").tenancyId(tenancy)
                .name("Weak Agent").slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .epistemicDomains(Map.of("rust", 0.15))
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();
        registry.register(desc);

        var status = capabilityHealth.probe(desc, "code-review",
                ProbeContext.of("rust"));

        assertThat(status).isInstanceOf(EpistemicallyWeak.class);
        var weak = (EpistemicallyWeak) status;
        assertThat(weak.domain()).isEqualTo("rust");
        assertThat(weak.confidence()).isEqualTo(0.15);
    }

    @Test
    void behavioral_violation_per_dimension() {
        var tenancy = "probe-violated-pd";
        var desc = codeReviewAgent("violated-agent-pd", tenancy);
        registry.register(desc);

        for (int i = 0; i < 3; i++) {
            signalStore.record(desc.agentId(), tenancy, "code-review",
                    ComplianceDimension.LATENCY, BehavioralSignal.VIOLATED);
        }

        var status = capabilityHealth.probe(desc, "code-review",
                ProbeContext.of("java"));

        assertThat(status).isInstanceOf(BehavioralViolation.class);
        var violation = (BehavioralViolation) status;
        assertThat(violation.kind())
                .isEqualTo(BehavioralViolation.ViolationKind.PER_DIMENSION);
        assertThat(violation.violations())
                .containsEntry(ComplianceDimension.LATENCY, 3);
    }

    @Test
    void behavioral_violation_aggregate() {
        var tenancy = "probe-violated-agg";
        var desc = codeReviewAgent("violated-agent-agg", tenancy);
        registry.register(desc);

        for (int i = 0; i < 2; i++) {
            signalStore.record(desc.agentId(), tenancy, "code-review",
                    ComplianceDimension.LATENCY, BehavioralSignal.VIOLATED);
            signalStore.record(desc.agentId(), tenancy, "code-review",
                    ComplianceDimension.DELEGATION, BehavioralSignal.VIOLATED);
            signalStore.record(desc.agentId(), tenancy, "code-review",
                    ComplianceDimension.ESCALATION, BehavioralSignal.VIOLATED);
        }

        var status = capabilityHealth.probe(desc, "code-review",
                ProbeContext.of("java"));

        assertThat(status).isInstanceOf(BehavioralViolation.class);
        var violation = (BehavioralViolation) status;
        assertThat(violation.kind())
                .isEqualTo(BehavioralViolation.ViolationKind.AGGREGATE);
        assertThat(violation.violations())
                .containsEntry(ComplianceDimension.LATENCY, 2)
                .containsEntry(ComplianceDimension.DELEGATION, 2)
                .containsEntry(ComplianceDimension.ESCALATION, 2);
    }

    @Test
    void healthy_agent_passes_all_checks() {
        var tenancy = "probe-healthy";
        var desc = codeReviewAgent("healthy-agent", tenancy);
        registry.register(desc);

        var status = capabilityHealth.probe(desc, "code-review",
                ProbeContext.of("java"));

        assertThat(status).isInstanceOf(Ready.class);
    }

    @Test
    void precedence_degraded_beats_exclusion() {
        var tenancy = "probe-precedence";
        var desc = AgentDescriptor.builder()
                .agentId("precedence-agent").tenancyId(tenancy)
                .name("Precedence Agent").slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .excludedDomains(Set.of("rust"))
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();
        registry.register(desc);

        stateStore.record(desc.agentId(), tenancy,
                DegradationReason.RATE_LIMITED,
                Instant.now().plus(1, ChronoUnit.HOURS));

        var status = capabilityHealth.probe(desc, "code-review",
                ProbeContext.of("rust"));

        assertThat(status).isInstanceOf(Degraded.class);
    }

    /**
     * Garden gotcha GE-20260523-fa7407: taskDomain is the subject domain
     * ("rust"), not the capability name ("code-review"). Passing the
     * capability name as taskDomain causes epistemicDomains lookup to miss.
     */
    @Test
    void probe_context_task_domain_vs_capability_tag() {
        var tenancy = "probe-context";
        var desc = AgentDescriptor.builder()
                .agentId("context-agent").tenancyId(tenancy)
                .name("Context Agent").slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .epistemicDomains(Map.of("rust", 0.15))
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();
        registry.register(desc);

        // CORRECT: taskDomain is the subject domain
        var correct = capabilityHealth.probe(desc, "code-review",
                ProbeContext.of("rust"));
        assertThat(correct).isInstanceOf(EpistemicallyWeak.class);

        // WRONG (but not an error): taskDomain = capability name
        // epistemicDomains doesn't have "code-review" → no weakness found → Ready
        var wrong = capabilityHealth.probe(desc, "code-review",
                ProbeContext.of("code-review"));
        assertThat(wrong).isInstanceOf(Ready.class);
    }
}
