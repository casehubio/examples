package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentMatch;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentQuery;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.vocab.JungianFunctionTerm;
import io.casehub.eidos.vocab.MbtiTypeTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates personality-aware agent dispatch: register agents with distinct
 * Jungian cognitive profiles, query by capability, and show how an orchestrator
 * would select agents based on A2A card personality data.
 *
 * The scenario: three code reviewers with the same capability but different
 * cognitive styles — a Ti-dominant analyst (systematic, logic-first), an
 * Fe-dominant collaborator (harmony-seeking, people-first), and a
 * Te-dominant executor (efficiency-focused, results-first). An orchestrator
 * querying for "code-review" gets all three, each carrying personality data
 * in their A2A card for informed dispatch.
 */
@QuarkusTest
class PersonalityAwareDispatchTest {

    @Inject AgentRegistry registry;
    @Inject VocabularyRegistry vocabRegistry;
    @Inject SystemPromptRenderer renderer;

    static final String TENANCY = "dispatch-example";

    static AgentDescriptor tiAnalyst() {
        return AgentDescriptor.builder()
                .agentId("ti-analyst").name("Analytical Reviewer").slot("reviewer").tenancyId(TENANCY)
                .dispositionVocabulary(JungianFunctionTerm.URI)
                .disposition(AgentDisposition.builder()
                        .dispositionProfile(MbtiTypeTerm.INTP.defaultProfile())
                        .build())
                .capabilities(List.of(AgentCapability.builder()
                        .name("code-review")
                        .description("Systematic logic-first code analysis")
                        .epistemicDomains(java.util.Map.of("java", 0.95, "rust", 0.4))
                        .build()))
                .build();
    }

    static AgentDescriptor feCollaborator() {
        return AgentDescriptor.builder()
                .agentId("fe-collaborator").name("Collaborative Reviewer").slot("reviewer").tenancyId(TENANCY)
                .dispositionVocabulary(JungianFunctionTerm.URI)
                .disposition(AgentDisposition.builder()
                        .dispositionProfile(MbtiTypeTerm.ENFJ.defaultProfile())
                        .build())
                .capabilities(List.of(AgentCapability.builder()
                        .name("code-review")
                        .description("Team-focused review emphasising knowledge sharing")
                        .epistemicDomains(java.util.Map.of("java", 0.8, "typescript", 0.85))
                        .build()))
                .build();
    }

    static AgentDescriptor teExecutor() {
        return AgentDescriptor.builder()
                .agentId("te-executor").name("Efficient Reviewer").slot("reviewer").tenancyId(TENANCY)
                .dispositionVocabulary(JungianFunctionTerm.URI)
                .disposition(AgentDisposition.builder()
                        .dispositionProfile(MbtiTypeTerm.ENTJ.defaultProfile())
                        .build())
                .capabilities(List.of(AgentCapability.builder()
                        .name("code-review")
                        .description("Results-driven review focused on efficiency and correctness")
                        .epistemicDomains(java.util.Map.of("java", 0.9, "go", 0.75))
                        .build()))
                .build();
    }

    @BeforeEach
    void setUp() {
        registry.register(tiAnalyst());
        registry.register(feCollaborator());
        registry.register(teExecutor());
    }

    @Test void all_three_reviewers_found_by_capability() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));
        assertThat(matches).hasSize(3);
        assertThat(matches).extracting(m -> m.descriptor().agentId())
                .containsExactlyInAnyOrder("ti-analyst", "fe-collaborator", "te-executor");
    }

    @Test void each_match_carries_resolved_capability() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));
        for (var match : matches) {
            assertThat(match.resolvedCapability()).isNotNull();
            assertThat(match.resolvedCapability().capability().name()).isEqualTo("code-review");
        }
    }

    @Test void each_reviewer_has_distinct_dominant_function() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));
        var dominants = matches.stream()
                .map(m -> m.descriptor().disposition().dispositionProfile().stream()
                        .max(java.util.Comparator.comparingDouble(dv -> dv.weight()))
                        .orElseThrow().term())
                .toList();
        assertThat(dominants).containsExactlyInAnyOrder("ti", "fe", "te");
    }

    @Test void a2a_cards_carry_distinct_personality_profiles() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));
        for (var match : matches) {
            var rendered = renderer.render(match.descriptor(),
                    AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
            assertThat(rendered.content()).contains("\"dispositionProfile\"");
            assertThat(rendered.content()).contains("urn:casehub:vocab:jungian");
        }
    }

    @Test void epistemic_domains_distinguish_language_strengths() {
        var matches = registry.find(AgentQuery.byCapability("code-review", TENANCY));
        var analyst = matches.stream()
                .filter(m -> m.descriptor().agentId().equals("ti-analyst")).findFirst().orElseThrow();
        var rendered = renderer.render(analyst.descriptor(),
                AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
        assertThat(rendered.content()).contains("\"java\"");
        assertThat(rendered.content()).contains("0.95");
    }

    @Test void slot_query_also_returns_all_reviewers() {
        var matches = registry.find(AgentQuery.bySlot("reviewer", TENANCY));
        assertThat(matches).hasSize(3);
        for (var match : matches) {
            assertThat(match.resolvedCapability()).isNull();
        }
    }
}
