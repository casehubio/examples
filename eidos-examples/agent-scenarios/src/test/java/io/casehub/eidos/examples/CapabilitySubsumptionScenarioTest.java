package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentQuery;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.api.MatchDegree;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.vocab.CasehubCapabilityTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for capability subsumption across the full pipeline:
 * AgentRegistry → VocabularyRegistry → CapabilityHealth.
 * <p>
 * Complements {@link CapabilityVocabularyIntegrationTest} which tests vocabulary
 * operations in isolation. This test exercises the complete agent discovery and
 * health check flow with vocabulary-grounded capabilities.
 */
@QuarkusTest
class CapabilitySubsumptionScenarioTest {

    @Inject
    AgentRegistry registry;

    @Inject
    VocabularyRegistry vocabularyRegistry;

    @Inject
    CapabilityHealth capabilityHealth;

    @Test
    void general_agent_found_when_querying_for_specific_capability() {
        var tenancyId = "tenant-general-to-specific";
        // Agent declares "code-review" with capabilityVocabulary
        var agent = AgentDescriptor.builder()
                .agentId("reviewer-general")
                .tenancyId(tenancyId)
                .name("General Code Reviewer")
                .slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();

        registry.register(agent);

        // Query for specific capability "security-code-review"
        var query = new AgentQuery(null, "security-code-review", tenancyId, null);
        var matches = registry.find(query);

        // Assert agent is in results (subsumption: code-review subsumes security-code-review)
        assertThat(matches)
                .extracting(m -> m.descriptor().agentId())
                .contains("reviewer-general");
    }

    @Test
    void specific_agent_found_when_querying_for_general_capability() {
        var tenancyId = "tenant-specific-to-general";
        // Agent declares "sast-review" with capabilityVocabulary
        var agent = AgentDescriptor.builder()
                .agentId("sast-specialist")
                .tenancyId(tenancyId)
                .name("SAST Review Specialist")
                .slot("security-reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("sast-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();

        registry.register(agent);

        // Query for general capability "code-review"
        var query = new AgentQuery(null, "code-review", tenancyId, null);
        var matches = registry.find(query);

        // Assert agent is in results (subsumption: sast-review is subsumed by code-review)
        assertThat(matches)
                .extracting(m -> m.descriptor().agentId())
                .contains("sast-specialist");
    }

    @Test
    void match_degree_reflects_hierarchy_depth() {
        // Verify VocabularyRegistry.match() returns correct Plugin/Specialization depths

        // Exact match → EXACT
        var exactMatch = vocabularyRegistry.match(
                CasehubCapabilityTerm.URI,
                "code-review",
                "code-review");
        assertThat(exactMatch).isInstanceOf(MatchDegree.Exact.class);

        // security-code-review specializes code-review (depth 1) → PLUGIN
        var pluginMatch = vocabularyRegistry.match(
                CasehubCapabilityTerm.URI,
                "code-review",
                "security-code-review");
        assertThat(pluginMatch).isInstanceOf(MatchDegree.Plugin.class);
        assertThat(((MatchDegree.Plugin) pluginMatch).depth()).isEqualTo(1);

        // sast-review specializes security-code-review (depth 1), which specializes code-review (depth 2) → PLUGIN
        var pluginMatchDepth2 = vocabularyRegistry.match(
                CasehubCapabilityTerm.URI,
                "code-review",
                "sast-review");
        assertThat(pluginMatchDepth2).isInstanceOf(MatchDegree.Plugin.class);
        assertThat(((MatchDegree.Plugin) pluginMatchDepth2).depth()).isEqualTo(2);

        // Inverse: code-review is generalized by sast-review → SPECIALIZATION
        var specializationMatch = vocabularyRegistry.match(
                CasehubCapabilityTerm.URI,
                "sast-review",
                "code-review");
        assertThat(specializationMatch).isInstanceOf(MatchDegree.Specialization.class);
        assertThat(((MatchDegree.Specialization) specializationMatch).depth()).isEqualTo(2);

        // Unrelated terms → NONE
        var noMatch = vocabularyRegistry.match(
                CasehubCapabilityTerm.URI,
                "code-review",
                "testing");
        assertThat(noMatch).isInstanceOf(MatchDegree.None.class);
    }

    @Test
    void health_probe_works_for_subsumption_discovered_agent() {
        var tenancyId = "tenant-probe-test";
        // Agent declares "code-review" grounded
        var agent = AgentDescriptor.builder()
                .agentId("reviewer-probe-test")
                .tenancyId(tenancyId)
                .name("Probe Test Reviewer")
                .slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();

        registry.register(agent);

        // Probe for specific capability "security-code-review"
        // This tests that CapabilityHealth supports subsumption matching
        var status = capabilityHealth.probe(
                agent,
                "security-code-review",
                new CapabilityHealth.ProbeContext(null, Map.of()));

        // Assert Ready (not Unavailable) — subsumption allows this
        assertThat(status).isInstanceOf(CapabilityHealth.CapabilityStatus.Ready.class);
    }

    @Test
    void ungrounded_capability_not_matched_by_subsumption() {
        var tenancyId = "tenant-ungrounded";
        // Agent declares "code-review" WITHOUT capabilityVocabulary
        var agent = AgentDescriptor.builder()
                .agentId("reviewer-ungrounded")
                .tenancyId(tenancyId)
                .name("Ungrounded Reviewer")
                .slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                // NO capabilityVocabulary set
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();

        registry.register(agent);

        // Query for "security-code-review"
        var query = new AgentQuery(null, "security-code-review", tenancyId, null);
        var matches = registry.find(query);

        // Assert NOT found (exact match only without vocabulary)
        assertThat(matches)
                .extracting(m -> m.descriptor().agentId())
                .doesNotContain("reviewer-ungrounded");
    }

    @Test
    void mixed_grounded_and_ungrounded_capabilities_filter_correctly() {
        var tenancyId = "tenant-mixed";
        // Agent with both grounded and ungrounded capabilities
        var agent = AgentDescriptor.builder()
                .agentId("mixed-capabilities")
                .tenancyId(tenancyId)
                .name("Mixed Capability Agent")
                .slot("reviewer")
                .capabilities(List.of(
                        // Grounded capability
                        AgentCapability.builder()
                                .name("code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .build(),
                        // Ungrounded capability
                        AgentCapability.builder()
                                .name("custom-review")
                                // NO capabilityVocabulary
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();

        registry.register(agent);

        // Query for "security-code-review" — should match via grounded "code-review"
        var securityQuery = new AgentQuery(null, "security-code-review", tenancyId, null);
        var securityMatches = registry.find(securityQuery);
        assertThat(securityMatches)
                .extracting(m -> m.descriptor().agentId())
                .contains("mixed-capabilities");

        // Query for "custom-review" — should match exact only
        var customQuery = new AgentQuery(null, "custom-review", tenancyId, null);
        var customMatches = registry.find(customQuery);
        assertThat(customMatches)
                .extracting(m -> m.descriptor().agentId())
                .contains("mixed-capabilities");

        // Query for "custom-review-specific" — should NOT match (ungrounded, no subsumption)
        var noMatchQuery = new AgentQuery(null, "custom-review-specific", tenancyId, null);
        var noMatches = registry.find(noMatchQuery);
        assertThat(noMatches)
                .extracting(m -> m.descriptor().agentId())
                .doesNotContain("mixed-capabilities");
    }

    @Test
    void multiple_agents_ranked_by_match_degree() {
        var tenancyId = "tenant-ranking";
        // Register three agents:
        // 1. Exact match: security-code-review
        var exactAgent = AgentDescriptor.builder()
                .agentId("security-exact")
                .tenancyId(tenancyId)
                .name("Security Code Reviewer")
                .slot("security-reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("security-code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();

        // 2. Specialization (depth 1): sast-review specializes security-code-review
        var specializationAgent = AgentDescriptor.builder()
                .agentId("sast-specialist")
                .tenancyId(tenancyId)
                .name("SAST Specialist")
                .slot("sast-reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("sast-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();

        // 3. Plugin (depth 1): code-review subsumes security-code-review
        var pluginAgent = AgentDescriptor.builder()
                .agentId("code-review-general")
                .tenancyId(tenancyId)
                .name("General Code Reviewer")
                .slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();

        registry.register(exactAgent);
        registry.register(specializationAgent);
        registry.register(pluginAgent);

        // Query for "security-code-review"
        var query = new AgentQuery(null, "security-code-review", tenancyId, null);
        var matches = registry.find(query);

        // All three should be in results, ordered by match quality: EXACT → PLUGIN → SPECIALIZATION
        assertThat(matches)
                .extracting(m -> m.descriptor().agentId())
                .containsExactly("security-exact", "code-review-general", "sast-specialist");

        // Verify match degrees
        assertThat(matches.get(0).resolvedCapability().degree())
                .isInstanceOf(MatchDegree.Exact.class);
        assertThat(matches.get(1).resolvedCapability().degree())
                .isInstanceOf(MatchDegree.Plugin.class);
        assertThat(matches.get(2).resolvedCapability().degree())
                .isInstanceOf(MatchDegree.Specialization.class);
    }

    @Test
    void excludedDomains_blocks_discovery_even_with_subsumption() {
        var tenancyId = "tenant-excluded-domains";
        // Agent declares "code-review" but excludes "java" domain
        var agent = AgentDescriptor.builder()
                .agentId("non-java-reviewer")
                .tenancyId(tenancyId)
                .name("Non-Java Code Reviewer")
                .slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .capabilityVocabulary(CasehubCapabilityTerm.URI)
                                .excludedDomains(Set.of("java"))
                                .build()))
                .disposition(AgentDisposition.builder().build())
                .build();

        registry.register(agent);

        // Query for "security-code-review" WITH taskDomain="java"
        var query = new AgentQuery(null, "security-code-review", tenancyId, "java");
        var matches = registry.find(query);

        // Assert NOT found (excluded by domain even though subsumption matches)
        assertThat(matches)
                .extracting(m -> m.descriptor().agentId())
                .doesNotContain("non-java-reviewer");

        // Query for "security-code-review" WITHOUT taskDomain
        var queryNoDomain = new AgentQuery(null, "security-code-review", tenancyId, null);
        var matchesNoDomain = registry.find(queryNoDomain);

        // Assert found (no domain filter applied)
        assertThat(matchesNoDomain)
                .extracting(m -> m.descriptor().agentId())
                .contains("non-java-reviewer");
    }
}
