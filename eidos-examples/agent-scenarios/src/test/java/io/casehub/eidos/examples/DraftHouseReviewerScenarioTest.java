package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DraftHouseReviewerScenarioTest {

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;

    // ── Registration (YAML-driven, no @BeforeEach) ────────────────────────────

    @Test
    void all_four_reviewers_found_by_slot() {
        var reviewers = registry.find(AgentQuery.bySlot("document-reviewer", "drafthouse"));
        assertThat(reviewers).hasSize(4);
        assertThat(reviewers.stream().map(m -> m.descriptor().agentId()).toList())
            .containsExactlyInAnyOrder(
                "drafthouse-structural-reviewer",
                "drafthouse-content-reviewer",
                "drafthouse-readability-reviewer",
                "drafthouse-completeness-reviewer");
    }

    @Test
    void all_four_reviewers_found_by_capability() {
        var reviewers = registry.find(AgentQuery.byCapability("document-review", "drafthouse"));
        assertThat(reviewers).hasSize(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "drafthouse-structural-reviewer",
        "drafthouse-content-reviewer",
        "drafthouse-readability-reviewer",
        "drafthouse-completeness-reviewer"
    })
    void each_reviewer_found_by_id(String agentId) {
        var desc = registry.findById(agentId, "drafthouse");
        assertThat(desc).isPresent();
        assertThat(desc.get().slot()).isEqualTo("document-reviewer");
        assertThat(desc.get().tenancyId()).isEqualTo("drafthouse");
        assertThat(desc.get().briefing()).isNotNull().isNotBlank();
        assertThat(desc.get().capabilities()).hasSize(1);
        assertThat(desc.get().capabilities().get(0).name()).isEqualTo("document-review");
    }

    @Test
    void tenancy_isolation_from_default() {
        var defaultReviewers = registry.find(
            AgentQuery.bySlot("document-reviewer", "default"));
        assertThat(defaultReviewers).isEmpty();
    }

    // ── Renderer (structural MARKDOWN — no LLM in examples module) ──────────

    @ParameterizedTest
    @ValueSource(strings = {
        "drafthouse-structural-reviewer",
        "drafthouse-content-reviewer",
        "drafthouse-readability-reviewer",
        "drafthouse-completeness-reviewer"
    })
    void rendered_output_contains_identity_and_capability(String agentId) {
        var desc = registry.findById(agentId, "drafthouse").orElseThrow();
        var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        var result = renderer.render(desc, ctx);

        assertThat(result.content()).contains(desc.name());
        assertThat(result.content()).contains("document-review");
        assertThat(result.format()).isEqualTo(RenderFormat.MARKDOWN);
    }

    @Test
    void structural_reviewer_shows_thomas_kilmann_label() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        var result = renderer.render(desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        assertThat(result.content()).contains("Collaborating (Thomas-Kilmann Conflict Modes)");
    }

    @Test
    void content_reviewer_shows_competing_label() {
        var desc = registry.findById("drafthouse-content-reviewer", "drafthouse").orElseThrow();
        var result = renderer.render(desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        assertThat(result.content()).contains("Competing (Thomas-Kilmann Conflict Modes)");
    }

    @Test
    void readability_reviewer_shows_accommodating_label() {
        var desc = registry.findById("drafthouse-readability-reviewer", "drafthouse").orElseThrow();
        var result = renderer.render(desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        assertThat(result.content()).contains("Accommodating (Thomas-Kilmann Conflict Modes)");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "drafthouse-structural-reviewer",
        "drafthouse-content-reviewer",
        "drafthouse-readability-reviewer",
        "drafthouse-completeness-reviewer"
    })
    void rendered_output_has_structural_markdown_sections(String agentId) {
        var desc = registry.findById(agentId, "drafthouse").orElseThrow();
        var result = renderer.render(desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        assertThat(result.content()).contains("## Role");
        assertThat(result.content()).contains("## How You Operate");
        assertThat(result.content()).contains("## Operating Principles");
    }

    @Test
    void structural_reviewer_briefing_in_rendered_output() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        var result = renderer.render(desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        assertThat(result.content()).contains("logical flow");
    }

    @Test
    void content_reviewer_briefing_in_rendered_output() {
        var desc = registry.findById("drafthouse-content-reviewer", "drafthouse").orElseThrow();
        var result = renderer.render(desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        assertThat(result.content()).contains("factual accuracy");
    }
}
