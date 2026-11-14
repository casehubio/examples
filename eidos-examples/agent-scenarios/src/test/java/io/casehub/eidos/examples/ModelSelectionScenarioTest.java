package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.vocab.ModelTierTerm;
import io.casehub.platform.api.model.CostTier;
import io.casehub.platform.api.model.ModelDescriptor;
import io.casehub.platform.api.model.ModelLocality;
import io.casehub.platform.api.model.ModelTier;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Model selection integration: agents declare model requirements via
 * modelTier and modelCapabilities on each capability. The platform's
 * ModelRegistry resolves concrete models. This scenario uses an inline
 * model catalog to demonstrate the full declaration → resolution flow.
 *
 * <p>Agents registered via META-INF/eidos/descriptors.yaml (YAML-driven).
 */
@QuarkusTest
class ModelSelectionScenarioTest {

    static final String TENANCY = "model-selection";

    @Inject AgentRegistry registry;
    @Inject VocabularyRegistry vocabRegistry;
    @Inject SystemPromptRenderer renderer;

    static final List<ModelDescriptor> MODEL_CATALOG = List.of(
        new ModelDescriptor("claude-opus-5", "claude-opus-5", "anthropic-vertex",
            "vertex-us", "Anthropic", "Claude", "Claude Opus 5",
            ModelTier.FLAGSHIP, Set.of("text", "vision", "tool-use"),
            1_000_000, 32_000, ModelLocality.CLOUD, CostTier.PREMIUM,
            "oauth2", Map.of()),
        new ModelDescriptor("claude-sonnet-5", "claude-sonnet-5", "anthropic-vertex",
            "vertex-us", "Anthropic", "Claude", "Claude Sonnet 5",
            ModelTier.STANDARD, Set.of("text", "vision", "tool-use"),
            200_000, 8_000, ModelLocality.CLOUD, CostTier.MEDIUM,
            "oauth2", Map.of()),
        new ModelDescriptor("claude-haiku-45", "claude-haiku-4-5", "anthropic-vertex",
            "vertex-us", "Anthropic", "Claude", "Claude Haiku 4.5",
            ModelTier.FAST, Set.of("text", "tool-use"),
            200_000, 8_000, ModelLocality.CLOUD, CostTier.LOW,
            "oauth2", Map.of()),
        new ModelDescriptor("voyage-3-large", "voyage-3-large", "voyageai",
            "voyage-us", "Voyage AI", "Voyage", "Voyage 3 Large",
            ModelTier.EMBEDDING, Set.of("text"),
            32_000, 0, ModelLocality.CLOUD, CostTier.LOW,
            "api-key", Map.of())
    );

    // ── Declaration: modelTier and modelCapabilities on descriptors ──

    @Test
    void flagship_analyst_declares_different_tiers_per_capability() {
        var desc = registry.findById("flagship-analyst", TENANCY).orElseThrow();
        var caps = desc.capabilities();

        var deepAnalysis = caps.stream()
            .filter(c -> c.name().equals("deep-analysis")).findFirst().orElseThrow();
        assertThat(deepAnalysis.model().tier()).isEqualTo(ModelTier.FLAGSHIP);
        assertThat(deepAnalysis.model().requiredCapabilities()).containsExactlyInAnyOrder("text", "tool-use");

        var docSummary = caps.stream()
            .filter(c -> c.name().equals("document-summary")).findFirst().orElseThrow();
        assertThat(docSummary.model().tier()).isEqualTo(ModelTier.STANDARD);
        assertThat(docSummary.model().requiredCapabilities()).containsExactly("text");
    }

    @Test
    void vision_reviewer_requires_vision_capability() {
        var desc = registry.findById("vision-reviewer", TENANCY).orElseThrow();
        var visualReview = desc.capabilities().stream()
            .filter(c -> c.name().equals("visual-code-review")).findFirst().orElseThrow();

        assertThat(visualReview.model().tier()).isEqualTo(ModelTier.FLAGSHIP);
        assertThat(visualReview.model().requiredCapabilities()).contains("vision");
    }

    @Test
    void embedding_indexer_declares_embedding_tier() {
        var desc = registry.findById("embedding-indexer", TENANCY).orElseThrow();
        var cap = desc.capabilities().getFirst();

        assertThat(cap.model().tier()).isEqualTo(ModelTier.EMBEDDING);
    }

    // ── Vocabulary subsumption: FLAGSHIP satisfies STANDARD ──────────

    @Test
    void flagship_satisfies_standard_via_subsumption() {
        var match = vocabRegistry.match(ModelTierTerm.URI, "flagship", "standard");
        assertThat(match).isInstanceOf(MatchDegree.Plugin.class);
        assertThat(((MatchDegree.Plugin) match).depth()).isEqualTo(1);
    }

    @Test
    void standard_is_specialization_of_flagship() {
        var match = vocabRegistry.match(ModelTierTerm.URI, "standard", "flagship");
        assertThat(match).isInstanceOf(MatchDegree.Specialization.class);
    }

    @Test
    void embedding_does_not_satisfy_standard() {
        var match = vocabRegistry.match(ModelTierTerm.URI, "embedding", "standard");
        assertThat(match).isInstanceOf(MatchDegree.None.class);
    }

    @Test
    void flagship_satisfies_fast_at_depth_2() {
        var match = vocabRegistry.match(ModelTierTerm.URI, "flagship", "fast");
        assertThat(match).isInstanceOf(MatchDegree.Plugin.class);
        assertThat(((MatchDegree.Plugin) match).depth()).isEqualTo(2);
    }

    // ── Resolution: matching agent requirements against model catalog ──

    @Test
    void resolve_flagship_text_vision_finds_opus() {
        var desc = registry.findById("vision-reviewer", TENANCY).orElseThrow();
        var cap = desc.capabilities().stream()
            .filter(c -> c.name().equals("visual-code-review")).findFirst().orElseThrow();

        var resolved = resolveModel(cap.model(), CostTier.PREMIUM);
        assertThat(resolved).isNotNull();
        assertThat(resolved.id()).isEqualTo("claude-opus-5");
    }

    @Test
    void resolve_standard_text_finds_sonnet() {
        var desc = registry.findById("flagship-analyst", TENANCY).orElseThrow();
        var cap = desc.capabilities().stream()
            .filter(c -> c.name().equals("document-summary")).findFirst().orElseThrow();

        var resolved = resolveModel(cap.model(), CostTier.PREMIUM);
        assertThat(resolved).isNotNull();
        assertThat(resolved.id()).isEqualTo("claude-sonnet-5");
    }

    @Test
    void resolve_fast_finds_haiku() {
        var desc = registry.findById("fast-linter", TENANCY).orElseThrow();
        var cap = desc.capabilities().getFirst();

        var resolved = resolveModel(cap.model(), CostTier.PREMIUM);
        assertThat(resolved).isNotNull();
        assertThat(resolved.id()).isEqualTo("claude-haiku-45");
    }

    @Test
    void resolve_embedding_finds_voyage() {
        var desc = registry.findById("embedding-indexer", TENANCY).orElseThrow();
        var cap = desc.capabilities().getFirst();

        var resolved = resolveModel(cap.model(), CostTier.PREMIUM);
        assertThat(resolved).isNotNull();
        assertThat(resolved.id()).isEqualTo("voyage-3-large");
    }

    // ── Cost-aware fallback: budget constrains tier selection ────────

    @Test
    void cost_budget_forces_fallback_from_flagship_to_standard() {
        var desc = registry.findById("flagship-analyst", TENANCY).orElseThrow();
        var cap = desc.capabilities().stream()
            .filter(c -> c.name().equals("deep-analysis")).findFirst().orElseThrow();

        var resolved = resolveModel(cap.model(), CostTier.MEDIUM);

        assertThat(resolved).isNotNull();
        assertThat(resolved.id()).isEqualTo("claude-sonnet-5");
        assertThat(resolved.tier()).isEqualTo(ModelTier.STANDARD);
    }

    @Test
    void tight_budget_forces_fallback_to_fast() {
        var desc = registry.findById("flagship-analyst", TENANCY).orElseThrow();
        var cap = desc.capabilities().stream()
            .filter(c -> c.name().equals("deep-analysis")).findFirst().orElseThrow();

        var resolved = resolveModel(cap.model(), CostTier.LOW);

        assertThat(resolved).isNotNull();
        assertThat(resolved.id()).isEqualTo("claude-haiku-45");
    }

    @Test
    void vision_requirement_prevents_fallback_to_haiku() {
        var desc = registry.findById("vision-reviewer", TENANCY).orElseThrow();
        var cap = desc.capabilities().stream()
            .filter(c -> c.name().equals("visual-code-review")).findFirst().orElseThrow();

        var resolved = resolveModel(cap.model(), CostTier.LOW);

        assertThat(resolved).isNull();
    }

    // ── A2A_CARD rendering includes model requirements ──────────────

    @Test
    void a2a_card_surfaces_model_tier_and_capabilities() {
        var desc = registry.findById("flagship-analyst", TENANCY).orElseThrow();
        var rendered = renderer.render(desc,
            AgentPromptContext.forFormat(RenderFormat.A2A_CARD));

        assertThat(rendered.content()).contains("\"model\"");
        assertThat(rendered.content()).contains("\"tier\"");
        assertThat(rendered.content()).contains("\"FLAGSHIP\"");
        assertThat(rendered.content()).contains("\"tool-use\"");
    }

    @Test
    void markdown_does_not_surface_model_tier() {
        var desc = registry.findById("fast-linter", TENANCY).orElseThrow();
        var rendered = renderer.render(desc,
            AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        assertThat(rendered.content()).doesNotContain("\"model\"");
        assertThat(rendered.content()).doesNotContain("\"tier\"");
    }

    // ── Resolution helper (simulates platform RoutingAgentProvider) ──

    private ModelDescriptor resolveModel(io.casehub.platform.api.model.ModelQuery query,
                                          CostTier maxCost) {
        ModelTier tier = query.tier();
        Set<String> requiredCapabilities = query.requiredCapabilities();
        String requiredTier = tier.name().toLowerCase();

        var exactMatches = MODEL_CATALOG.stream()
            .filter(m -> m.tier() == tier)
            .filter(m -> m.capabilities().containsAll(requiredCapabilities))
            .filter(m -> m.costTier().rank() <= maxCost.rank())
            .toList();

        if (!exactMatches.isEmpty()) return exactMatches.getFirst();

        return MODEL_CATALOG.stream()
            .filter(m -> m.capabilities().containsAll(requiredCapabilities))
            .filter(m -> m.costTier().rank() <= maxCost.rank())
            .filter(m -> {
                var match = vocabRegistry.match(ModelTierTerm.URI,
                    m.tier().name().toLowerCase(), requiredTier);
                return !(match instanceof MatchDegree.None);
            })
            .min((a, b) -> {
                var matchA = vocabRegistry.match(ModelTierTerm.URI,
                    a.tier().name().toLowerCase(), requiredTier);
                var matchB = vocabRegistry.match(ModelTierTerm.URI,
                    b.tier().name().toLowerCase(), requiredTier);
                return matchA.compareTo(matchB);
            })
            .orElse(null);
    }
}
