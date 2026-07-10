package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class MultiAgentTeamTest {

    @Inject AgentRegistry registry;

    @BeforeEach
    void registerTeam() {
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
                .inputTypes(List.of("requirements")).outputTypes(List.of("plan")).tags(List.of("orchestration"))
                .epistemicDomains(Map.of("software", 0.95, "logistics", 0.4)).build()))
            .disposition(AgentDisposition.builder()
                .socialOrient("facilitative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .delegation(true)
                .build())
            .tenancyId("default")
            .build());

        registry.register(AgentDescriptor.builder()
            .agentId("reviewer-1")
            .name("Code Reviewer")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7-sonnet")
            .slotVocabulary("urn:casehub:vocab:casehub-slot")
            .slot("reviewer")
            .capabilities(List.of(
                AgentCapability.builder().name("code-review").qualityHint(0.95).latencyHintP50Ms(150L).costHint("low")
                    .inputTypes(List.of("code")).outputTypes(List.of("review")).tags(List.of("quality"))
                    .epistemicDomains(Map.of("java", 0.95, "python", 0.8, "rust", 0.3)).build(),
                AgentCapability.builder().name("test-writing").qualityHint(0.8).latencyHintP50Ms(300L).costHint("medium")
                    .inputTypes(List.of("code")).outputTypes(List.of("tests")).tags(List.of("testing"))
                    .epistemicDomains(Map.of("java", 0.9)).build()))
            .disposition(AgentDisposition.builder()
                .socialOrient("independent")
                .ruleFollowing("strict")
                .riskAppetite("conservative")
                .autonomy("directed")
                .build())
            .tenancyId("default")
            .build());

        registry.register(AgentDescriptor.builder()
            .agentId("executor-1")
            .name("Task Executor")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7-sonnet")
            .slotVocabulary("urn:casehub:vocab:casehub-slot")
            .slot("executor")
            .capabilities(List.of(AgentCapability.builder()
                .name("code-generation").qualityHint(0.85).latencyHintP50Ms(500L).costHint("high")
                .inputTypes(List.of("spec")).outputTypes(List.of("code")).tags(List.of("implementation"))
                .epistemicDomains(Map.of("java", 0.9, "python", 0.85, "rust", 0.6)).build()))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("flexible")
                .riskAppetite("bold")
                .autonomy("autonomous")
                .build())
            .tenancyId("default")
            .build());
    }

    @Test
    void find_by_id_returns_complete_descriptor() {
        var planner = registry.findById("planner-1", "default");
        assertThat(planner).isPresent();
        assertThat(planner.get().name()).isEqualTo("Strategic Planner");
        assertThat(planner.get().slot()).isEqualTo("planner");
        assertThat(planner.get().disposition().delegation()).isTrue();
        assertThat(planner.get().capabilities()).hasSize(1);
        assertThat(planner.get().capabilities().get(0).epistemicDomains())
            .containsEntry("software", 0.95);
    }

    @Test
    void find_reviewers_by_slot() {
        var reviewers = registry.find(AgentQuery.bySlot("reviewer", "default"));
        assertThat(reviewers).hasSize(1);
        assertThat(reviewers.get(0).descriptor().agentId()).isEqualTo("reviewer-1");
    }

    @Test
    void find_agents_with_code_review_capability() {
        var agents = registry.find(AgentQuery.byCapability("code-review", "default"));
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0).descriptor().agentId()).isEqualTo("reviewer-1");
    }

    @Test
    void find_executor_by_slot_and_capability() {
        var executors = registry.find(
            AgentQuery.bySlotAndCapability("executor", "code-generation", "default"));
        assertThat(executors).hasSize(1);
        assertThat(executors.get(0).descriptor().agentId()).isEqualTo("executor-1");
    }

    @Test
    void find_all_returns_entire_team() {
        var all = registry.find(AgentQuery.all("default"));
        assertThat(all).hasSizeGreaterThanOrEqualTo(3);
        assertThat(all.stream().map(m -> m.descriptor().agentId()).toList())
            .contains("planner-1", "reviewer-1", "executor-1");
    }

    @Test
    void agent_with_multiple_capabilities_found_by_either() {
        var codeReviewers = registry.find(AgentQuery.byCapability("code-review", "default"));
        var testWriters = registry.find(AgentQuery.byCapability("test-writing", "default"));
        assertThat(codeReviewers.stream().map(m -> m.descriptor().agentId()).toList())
            .contains("reviewer-1");
        assertThat(testWriters.stream().map(m -> m.descriptor().agentId()).toList())
            .contains("reviewer-1");
    }
}
