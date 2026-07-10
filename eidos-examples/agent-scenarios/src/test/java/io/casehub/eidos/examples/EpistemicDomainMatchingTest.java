package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class EpistemicDomainMatchingTest {

    @Inject CapabilityHealth health;

    final AgentDescriptor polyglotReviewer = AgentDescriptor.builder()
        .agentId("polyglot-1")
        .name("Polyglot Reviewer")
        .version("1.0")
        .provider("anthropic")
        .modelFamily("claude")
        .modelVersion("claude-3-7-sonnet")
        .slot("reviewer")
        .capabilities(List.of(AgentCapability.builder()
            .name("code-review").qualityHint(0.9).latencyHintP50Ms(150L).costHint("low")
            .inputTypes(List.of("code")).outputTypes(List.of("review")).tags(List.of("quality"))
            .epistemicDomains(Map.of("java", 0.95, "python", 0.8, "rust", 0.2)).build()))
        .disposition(AgentDisposition.builder()
            .socialOrient("independent")
            .ruleFollowing("principled")
            .riskAppetite("measured")
            .autonomy("directed")
            .build())
        .tenancyId("default")
        .build();

    @Test
    void probe_ready_for_strong_domain() {
        var status = health.probe(polyglotReviewer, "code-review", ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void probe_epistemically_weak_for_low_confidence_domain() {
        var status = health.probe(polyglotReviewer, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.EpistemicallyWeak.class);
        var weak = (CapabilityStatus.EpistemicallyWeak) status;
        assertThat(weak.domain()).isEqualTo("rust");
        assertThat(weak.confidence()).isEqualTo(0.2);
    }

    @Test
    void probe_ready_for_unknown_domain() {
        var status = health.probe(polyglotReviewer, "code-review", ProbeContext.of("haskell"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void probe_unavailable_for_undeclared_capability() {
        var status = health.probe(polyglotReviewer, "penetration-testing", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Unavailable.class);
        assertThat(((CapabilityStatus.Unavailable) status).reason())
            .contains("penetration-testing");
    }
}
