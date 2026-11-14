package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cost-aware multi-agent routing: three code-review agents with different
 * quality/latency/cost profiles. Consumer queries by capability, ranks by
 * routing signals, and filters by task domain — all inline, no framework.
 *
 * <p>Agents registered via META-INF/eidos/descriptors.yaml (YAML-driven).
 */
@QuarkusTest
class CostAwareRoutingScenarioTest {

    static final String TENANCY = "cost-routing";

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;

    // ── Discovery ────────────────────────────────────────────────────────

    @Test
    void all_three_found_by_foundation_capability() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));

        assertThat(matches).hasSize(3);
        assertThat(matches).extracting(m -> m.descriptor().agentId())
                .containsExactlyInAnyOrder(
                        "premium-reviewer", "standard-reviewer", "security-specialist");
    }

    @Test
    void matches_ordered_by_match_degree() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));

        var exactCount = matches.stream()
                .filter(m -> m.resolvedCapability().degree() instanceof MatchDegree.Exact)
                .count();
        assertThat(exactCount).isEqualTo(2);

        var last = matches.get(matches.size() - 1);
        assertThat(last.descriptor().agentId()).isEqualTo("security-specialist");
        assertThat(last.resolvedCapability().degree())
                .isInstanceOf(MatchDegree.Specialization.class);
    }

    @Test
    void each_match_carries_routing_signals() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));

        for (var match : matches) {
            assertThat(match.resolvedCapability()).isNotNull();
            var cap = match.resolvedCapability().capability();
            assertThat(cap.qualityHint()).isNotNull();
            assertThat(cap.latencyHintP50Ms()).isNotNull();
            assertThat(cap.costHint()).isNotNull();
        }
    }

    // ── Domain filtering ─────────────────────────────────────────────────

    @Test
    void task_domain_excludes_declared_exclusions() {
        var matches = registry.find(
                AgentQuery.byCapabilityAndDomain("code-review", "rust", TENANCY));

        assertThat(matches).extracting(m -> m.descriptor().agentId())
                .doesNotContain("security-specialist")
                .contains("premium-reviewer", "standard-reviewer");
    }

    @Test
    void task_domain_without_exclusion_returns_all() {
        var matches = registry.find(
                AgentQuery.byCapabilityAndDomain("code-review", "java", TENANCY));

        assertThat(matches).hasSize(3);
    }

    // ── Consumer-side ranking (inline logic) ─────────────────────────────

    @Test
    void rank_by_quality_for_critical_review() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));

        var ranked = matches.stream()
                .sorted(Comparator.comparingDouble(
                        (AgentMatch m) -> m.resolvedCapability().capability().qualityHint())
                        .reversed())
                .toList();

        assertThat(ranked.get(0).descriptor().agentId()).isEqualTo("premium-reviewer");
        assertThat(ranked.get(ranked.size() - 1).descriptor().agentId())
                .isEqualTo("standard-reviewer");
    }

    @Test
    void rank_by_latency_for_time_sensitive_task() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));

        var fast = matches.stream()
                .filter(m -> m.resolvedCapability().capability().latencyHintP50Ms() <= 5000)
                .sorted(Comparator.comparingLong(
                        (AgentMatch m) -> m.resolvedCapability().capability().latencyHintP50Ms()))
                .toList();

        assertThat(fast).hasSize(2);
        assertThat(fast.get(0).descriptor().agentId()).isEqualTo("standard-reviewer");
    }

    @Test
    void filter_by_cost_tier() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));

        var standard = matches.stream()
                .filter(m -> "standard".equals(
                        m.resolvedCapability().capability().costHint()))
                .toList();

        assertThat(standard).hasSize(1);
        assertThat(standard.get(0).descriptor().agentId())
                .isEqualTo("standard-reviewer");
    }

    @Test
    void combined_ranking_quality_within_latency_budget() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));

        var withinBudget = matches.stream()
                .filter(m -> m.resolvedCapability().capability().latencyHintP50Ms() <= 10000)
                .sorted(Comparator.comparingDouble(
                        (AgentMatch m) -> m.resolvedCapability().capability().qualityHint())
                        .reversed())
                .toList();

        assertThat(withinBudget).hasSize(2);
        assertThat(withinBudget.get(0).descriptor().agentId())
                .isEqualTo("security-specialist");
        assertThat(withinBudget.get(1).descriptor().agentId())
                .isEqualTo("standard-reviewer");
    }

    // ── A2A_CARD rendering ───────────────────────────────────────────────

    @Test
    void a2a_card_surfaces_routing_signals() {
        var desc = registry.findById("premium-reviewer", TENANCY).orElseThrow();
        var rendered = renderer.render(desc,
                AgentPromptContext.forFormat(RenderFormat.A2A_CARD));

        assertThat(rendered.content()).contains("\"qualityHint\"");
        assertThat(rendered.content()).contains("0.95");
        assertThat(rendered.content()).contains("\"latencyHintP50Ms\"");
        assertThat(rendered.content()).contains("30000");
        assertThat(rendered.content()).contains("\"costHint\"");
        assertThat(rendered.content()).contains("\"premium\"");
        assertThat(rendered.content()).contains("\"epistemicDomains\"");
    }
}
