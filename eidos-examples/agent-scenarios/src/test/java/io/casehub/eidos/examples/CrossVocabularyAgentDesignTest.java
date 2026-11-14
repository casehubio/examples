package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.Visibility;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.vocab.BelbinTerm;
import io.casehub.eidos.vocab.JungianFunctionTerm;
import io.casehub.eidos.vocab.MbtiTypeTerm;
import io.casehub.eidos.vocab.ThomasKilmannTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates cross-vocabulary agent composition: Belbin team role (slot) +
 * Jungian cognitive profile (disposition) + capabilities + goals + constraints,
 * rendered across MARKDOWN, PROSE, and A2A_CARD formats.
 *
 * This is the recommended pattern for building agents that carry both
 * "what they contribute to the team" (Belbin) and "how they think" (Jung)
 * as independent, composable signals.
 */
@QuarkusTest
class CrossVocabularyAgentDesignTest {

    @Inject AgentRegistry registry;
    @Inject VocabularyRegistry vocabRegistry;
    @Inject SystemPromptRenderer renderer;

    static final String TENANCY = "cross-vocab-design";

    static AgentDescriptor compositeArchitect() {
        return AgentDescriptor.builder()
                .agentId("composite-architect")
                .name("Solution Architect")
                .slot("co-ordinator")
                .tenancyId(TENANCY)
                .slotVocabulary(BelbinTerm.URI)
                .dispositionVocabulary(JungianFunctionTerm.URI)
                .disposition(AgentDisposition.builder()
                        .dispositionProfile(MbtiTypeTerm.INTJ.defaultProfile())
                        .delegation(true)
                        .build())
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("system-design")
                                .description("Designs distributed system architectures")
                                .build(),
                        AgentCapability.builder()
                                .name("code-review")
                                .description("Reviews code for architectural consistency")
                                .epistemicDomains(java.util.Map.of("java", 0.95, "python", 0.7))
                                .build()))
                .goals(List.of(
                        new AgentGoal("architectural-coherence", "Maintain system-wide architectural consistency",
                                GoalPriority.PRIMARY, Visibility.PUBLIC, List.of(), null),
                        new AgentGoal("knowledge-transfer", "Ensure team understands design rationale",
                                GoalPriority.SECONDARY, Visibility.PUBLIC, List.of(), null)))
                .constraints(List.of(
                        new AgentConstraint("no-premature-optimisation", "Do not optimise before measuring",
                                Visibility.PUBLIC, ConstraintSeverity.HARD),
                        new AgentConstraint("prefer-simplicity", "Choose the simpler design when trade-offs are close",
                                Visibility.PUBLIC, ConstraintSeverity.SOFT)))
                .build();
    }

    @BeforeEach
    void setUp() {
        registry.register(compositeArchitect());
    }

    @Test
    void descriptor_combines_belbin_slot_and_jungian_disposition() {
        var desc = registry.findById("composite-architect", TENANCY).orElseThrow();
        assertThat(desc.slot()).isEqualTo("co-ordinator");
        assertThat(desc.disposition().dispositionProfile()).isNotEmpty();
        assertThat(desc.disposition().delegation()).isTrue();
    }

    @Test
    void belbin_slot_resolves_via_vocabulary_registry() {
        var term = vocabRegistry.resolve(BelbinTerm.URI, "co-ordinator");
        assertThat(term).isPresent();
        assertThat(term.get().label()).isEqualTo("Co-ordinator");
    }

    @Test
    void jungian_profile_has_ni_dominant() {
        var desc = registry.findById("composite-architect", TENANCY).orElseThrow();
        var profile = desc.disposition().dispositionProfile();
        var dominant = profile.stream()
                .max(java.util.Comparator.comparingDouble(dv -> dv.weight()))
                .orElseThrow();
        assertThat(dominant.term()).isEqualTo("ni");
    }

    @Test
    void markdown_renders_both_role_and_cognitive_style() {
        var desc = registry.findById("composite-architect", TENANCY).orElseThrow();
        var rendered = renderer.render(desc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
        assertThat(rendered.content()).contains("Co-ordinator");
        assertThat(rendered.content()).contains("[HARD]");
        assertThat(rendered.content()).contains("[SOFT]");
        assertThat(rendered.content()).contains("[PRIMARY]");
    }

    @Test
    void prose_renders_goals_and_constraints_with_severity() {
        var desc = registry.findById("composite-architect", TENANCY).orElseThrow();
        var rendered = renderer.render(desc, AgentPromptContext.forFormat(RenderFormat.PROSE));
        assertThat(rendered.content()).contains("Hard constraints:");
        assertThat(rendered.content()).contains("Maintain system-wide architectural consistency");
    }

    @Test
    void a2a_card_carries_slot_disposition_goals_constraints_capabilities() {
        var desc = registry.findById("composite-architect", TENANCY).orElseThrow();
        var rendered = renderer.render(desc, AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
        var json = rendered.content();
        assertThat(json).contains("\"slot\"");
        assertThat(json).contains("\"co-ordinator\"");
        assertThat(json).contains("\"goals\"");
        assertThat(json).contains("\"constraints\"");
        assertThat(json).contains("\"severity\":\"HARD\"");
        assertThat(json).contains("\"capabilities\"");
        assertThat(json).contains("\"system-design\"");
        assertThat(json).contains("\"frameworks\"");
    }

    @Test
    void a2a_card_has_belbin_in_frameworks_and_jungian_in_disposition_profile() {
        var desc = registry.findById("composite-architect", TENANCY).orElseThrow();
        var rendered = renderer.render(desc, AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
        assertThat(rendered.content()).contains("Belbin Team Roles");
        assertThat(rendered.content()).contains("urn:casehub:vocab:jungian");
        assertThat(rendered.content()).contains("INTJ");
    }

    @Test
    void has_goal_and_has_constraint_convenience_methods() {
        var desc = registry.findById("composite-architect", TENANCY).orElseThrow();
        assertThat(desc.hasGoal("architectural-coherence")).isTrue();
        assertThat(desc.hasGoal("nonexistent")).isFalse();
        assertThat(desc.hasConstraint("no-premature-optimisation")).isTrue();
        assertThat(desc.hasConstraint("nonexistent")).isFalse();
    }
}
