package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class SystemPromptRendererTest {

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject VocabularyRegistry vocabularyRegistry;

    @BeforeEach
    void registerAgent() {
        registry.register(AgentDescriptor.builder()
            .agentId("planner-1")
            .name("Strategic Planner")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7-sonnet")
            .slotVocabulary("urn:casehub:vocab:casehub-slot")
            .slot("planner")
            .capabilities(List.of(AgentCapability.builder()
                .name("planning").qualityHint(0.9).latencyHintP50Ms(200L).costHint("medium")
                .inputTypes(List.of("requirements")).outputTypes(List.of("plan")).tags(List.of())
                .epistemicDomains(Map.of("software", 0.95, "logistics", 0.4)).build()))
            .disposition(AgentDisposition.builder()
                .socialOrient("facilitative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .delegation(true)
                .build())
            .jurisdiction("EU")
            .dataHandlingPolicy("gdpr-compliant")
            .tenancyId("default")
            .build());
    }

    @Test
    void renders_agent_without_goal() {
        final var descriptor = registry.findById("planner-1", "default").orElseThrow();
        final var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);

        final var result = renderer.render(descriptor, context);

        assertThat(result.content()).contains("Strategic Planner");
        assertThat(result.content()).contains("planning");
        assertThat(result.format()).isEqualTo(RenderFormat.MARKDOWN);
        assertThat(result.descriptorHash()).isNotBlank();
        assertThat(result.contextHash()).isNotBlank();
    }

    @Test
    void renders_agent_with_goal_and_resources() {
        final var descriptor = registry.findById("planner-1", "default").orElseThrow();
        final var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN)
                .withGoal(new GoalContext("Plan the Q3 release", List.of("Define milestones", "Assign owners"), "case-q3"))
                .withResources(List.of(new Resource("https://jira.example.com/q3", "Q3 board", "web")))
                .withSituationalContext("EOQ sprint planning session");

        final var result = renderer.render(descriptor, context);

        assertThat(result.content()).contains("Strategic Planner");
        assertThat(result.content()).contains("Plan the Q3 release");
        assertThat(result.content()).contains("jira.example.com");
        assertThat(result.content()).contains("EOQ sprint planning session");
    }

    @Test
    void hash_changes_when_context_changes() {
        final var descriptor = registry.findById("planner-1", "default").orElseThrow();
        final var ctx1 = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        final var ctx2 = AgentPromptContext.forFormat(RenderFormat.MARKDOWN)
                .withSituationalContext("different context");

        final var r1 = renderer.render(descriptor, ctx1);
        final var r2 = renderer.render(descriptor, ctx2);

        assertThat(r1.descriptorHash()).isEqualTo(r2.descriptorHash());
        assertThat(r1.contextHash()).isNotEqualTo(r2.contextHash());
    }

    @Test
    void conscientiousness_disposition_shows_vocab_resolved_labels() {
        registry.register(AgentDescriptor.builder()
            .agentId("cons-agent").name("Conscientiousness Agent")
            .slot("reviewer")
            .dispositionVocabulary("urn:casehub:vocab:conscientiousness")
            .disposition(AgentDisposition.builder()
                .socialOrient("facilitative")
                .ruleFollowing("principled")
                .build())
            .tenancyId("default").build());

        final var descriptor = registry.findById("cons-agent", "default").orElseThrow();
        final var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        final var result = renderer.render(descriptor, context);

        assertThat(result.content()).contains("Facilitative (Conscientiousness Disposition Axes)");
        assertThat(result.content()).contains("Principled (Conscientiousness Disposition Axes)");
        // Raw values must not appear as plain text
        assertThat(result.content()).doesNotContain("Social orientation: facilitative\n");
    }

    @Test
    void thomas_kilmann_conflict_mode_appears_in_structural_output() {
        registry.register(AgentDescriptor.builder()
            .agentId("tk-agent").name("TK Agent")
            .slot("reviewer")
            .dispositionVocabulary("urn:casehub:vocab:thomas-kilmann")
            .disposition(AgentDisposition.builder()
                .conflictMode("collaborating")
                .build())
            .tenancyId("default").build());

        final var descriptor = registry.findById("tk-agent", "default").orElseThrow();
        final var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        final var result = renderer.render(descriptor, context);

        assertThat(result.content()).contains("Conflict mode:");
        assertThat(result.content()).contains("Collaborating (Thomas-Kilmann Conflict Modes)");
    }

    // ── A2A card framework references (eidos#45) ───────────────────────────────

    private com.fasterxml.jackson.databind.JsonNode renderA2aCard(final AgentDescriptor descriptor) {
        try {
            final var result = renderer.render(descriptor, AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.content());
        } catch (final Exception e) {
            throw new IllegalStateException("A2A card is not valid JSON", e);
        }
    }

    @Test
    void a2a_card_belbin_slot_exposes_slot_and_framework() {
        final var descriptor = AgentDescriptor.builder()
            .agentId("belbin-1").name("Monitor Evaluator")
            .slotVocabulary("urn:casehub:vocab:belbin")
            .slot("monitor-evaluator")
            .tenancyId("default").build();
        registry.register(descriptor);

        final var card = renderA2aCard(registry.findById("belbin-1", "default").orElseThrow());
        final var slot = card.get("slot");
        assertThat(slot.get("value").asText()).isEqualTo("monitor-evaluator");
        assertThat(slot.get("label").asText()).isEqualTo("Monitor Evaluator");
        assertThat(slot.get("vocabularyUri").asText()).isEqualTo("urn:casehub:vocab:belbin");
        assertThat(slot.get("vocabularyName").asText()).isEqualTo("Belbin Team Roles");

        final var frameworks = card.get("frameworks");
        assertThat(frameworks.isArray()).isTrue();
        assertThat(frameworks.get(0).get("uri").asText()).isEqualTo("urn:casehub:vocab:belbin");
        assertThat(frameworks.get(0).get("name").asText()).isEqualTo("Belbin Team Roles");
    }

    @Test
    void a2a_card_conscientiousness_disposition_exposes_framework() {
        final var descriptor = AgentDescriptor.builder()
            .agentId("cons-a2a-1").name("Conscientiousness Agent")
            .slot("reviewer")
            .dispositionVocabulary("urn:casehub:vocab:conscientiousness")
            .disposition(AgentDisposition.builder()
                .socialOrient("independent")
                .ruleFollowing("strict")
                .build())
            .tenancyId("default").build();
        registry.register(descriptor);

        final var card = renderA2aCard(registry.findById("cons-a2a-1", "default").orElseThrow());
        final var socialOrient = card.get("disposition").get("socialOrient");
        assertThat(socialOrient.get("vocabularyUri").asText())
            .isEqualTo("urn:casehub:vocab:conscientiousness");
        assertThat(socialOrient.get("vocabularyName").asText())
            .isEqualTo("Conscientiousness Disposition Axes");

        final var frameworks = card.get("frameworks");
        assertThat(frameworks.isArray()).isTrue();
        assertThat(frameworks).anyMatch(f ->
            "urn:casehub:vocab:conscientiousness".equals(f.get("uri").asText()));
    }

    @Test
    void a2a_card_thomas_kilmann_conflict_mode_in_frameworks() {
        final var descriptor = AgentDescriptor.builder()
            .agentId("tk-a2a-1").name("TK Agent")
            .slot("reviewer")
            .axisVocabularies(Map.of(DispositionAxis.CONFLICT_MODE,
                "urn:casehub:vocab:thomas-kilmann"))
            .disposition(AgentDisposition.builder().conflictMode("collaborating").build())
            .tenancyId("default").build();
        registry.register(descriptor);

        final var card = renderA2aCard(registry.findById("tk-a2a-1", "default").orElseThrow());
        final var conflictMode = card.get("disposition").get("conflictMode");
        assertThat(conflictMode.get("value").asText()).isEqualTo("collaborating");
        assertThat(conflictMode.get("label").asText()).isEqualTo("Collaborating");
        assertThat(conflictMode.get("vocabularyUri").asText())
            .isEqualTo("urn:casehub:vocab:thomas-kilmann");
        assertThat(conflictMode.get("vocabularyName").asText())
            .isEqualTo("Thomas-Kilmann Conflict Modes");

        final var frameworks = card.get("frameworks");
        assertThat(frameworks.isArray()).isTrue();
        assertThat(frameworks.get(0).get("uri").asText())
            .isEqualTo("urn:casehub:vocab:thomas-kilmann");
    }

    @Test
    void a2a_card_slot_no_vocab_disposition_has_vocab() {
        final var descriptor = AgentDescriptor.builder()
            .agentId("mixed-1").name("Mixed Agent")
            .slot("reviewer") // no slotVocabulary
            .dispositionVocabulary("urn:casehub:vocab:conscientiousness")
            .disposition(AgentDisposition.builder().socialOrient("independent").build())
            .tenancyId("default").build();
        registry.register(descriptor);

        final var card = renderA2aCard(registry.findById("mixed-1", "default").orElseThrow());
        // slot has value only — no vocab fields
        final var slot = card.get("slot");
        assertThat(slot.get("value").asText()).isEqualTo("reviewer");
        assertThat(slot.has("vocabularyUri")).isFalse();
        assertThat(slot.has("label")).isFalse();
        // disposition has vocab context
        assertThat(card.get("disposition").get("socialOrient").get("vocabularyName").asText())
            .isEqualTo("Conscientiousness Disposition Axes");
        // frameworks sourced from disposition only
        final var frameworks = card.get("frameworks");
        assertThat(frameworks.isArray()).isTrue();
        assertThat(frameworks.size()).isEqualTo(1);
        assertThat(frameworks.get(0).get("uri").asText())
            .isEqualTo("urn:casehub:vocab:conscientiousness");
    }

    @Test
    void a2a_card_no_vocab_agent_has_no_frameworks_key() {
        final var descriptor = AgentDescriptor.builder()
            .agentId("plain-1").name("Plain Agent")
            .slot("reviewer") // no vocabulary URIs configured at all
            .tenancyId("default").build();
        registry.register(descriptor);

        final var card = renderA2aCard(registry.findById("plain-1", "default").orElseThrow());
        assertThat(card.has("frameworks")).isFalse();
        // slot has value but no vocab fields
        assertThat(card.get("slot").has("vocabularyUri")).isFalse();
    }

    @Test
    void descriptor_with_vocab_uri_has_different_hash_from_descriptor_without() {
        // Guard: if the vocab registrar didn't run, this test produces a misleading failure
        assertThat(vocabularyRegistry.isRegistered("urn:casehub:vocab:conscientiousness"))
            .as("Conscientiousness vocab must be CDI-registered via VocabularyRegistrar beans")
            .isTrue();
        registry.register(AgentDescriptor.builder()
            .agentId("hash-vocab").name("Agent").slot("reviewer")
            .dispositionVocabulary("urn:casehub:vocab:conscientiousness")
            .disposition(AgentDisposition.builder().socialOrient("facilitative").build())
            .tenancyId("default").build());
        registry.register(AgentDescriptor.builder()
            .agentId("hash-plain").name("Agent").slot("reviewer")
            .disposition(AgentDisposition.builder().socialOrient("facilitative").build())
            .tenancyId("default").build());

        final var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        final var resultVocab = renderer.render(
            registry.findById("hash-vocab", "default").orElseThrow(), ctx);
        final var resultPlain = renderer.render(
            registry.findById("hash-plain", "default").orElseThrow(), ctx);

        // Registered vocab adds vocabularyName to payload → different descriptor hash
        assertThat(resultVocab.descriptorHash()).isNotEqualTo(resultPlain.descriptorHash());
    }
}
