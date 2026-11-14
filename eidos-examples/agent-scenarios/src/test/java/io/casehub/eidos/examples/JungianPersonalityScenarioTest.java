package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.DispositionEvolution;
import io.casehub.eidos.api.DispositionEvolution.EvolutionResult;
import io.casehub.eidos.api.DispositionHealth;
import io.casehub.eidos.api.DispositionHealth.DispositionStatus;
import io.casehub.eidos.api.DispositionSignalStore;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.vocab.JungianFunctionTerm;
import io.casehub.eidos.vocab.MbtiTypeTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@QuarkusTest
class JungianPersonalityScenarioTest {

    @Inject AgentRegistry registry;
    @Inject VocabularyRegistry vocabRegistry;
    @Inject SystemPromptRenderer renderer;
    @Inject DispositionHealth dispositionHealth;
    @Inject DispositionEvolution dispositionEvolution;
    @Inject DispositionSignalStore signalStore;

    static final String TENANCY = "jungian-examples";

    @BeforeEach
    void setUp() {
        signalStore.clear("intp-agent", TENANCY);
    }

    static AgentDescriptor intpAnalyst() {
        return AgentDescriptor.builder()
                .agentId("intp-agent").name("Systems Analyst (INTP)")
                .slot("analyst").tenancyId(TENANCY)
                .dispositionVocabulary(JungianFunctionTerm.URI)
                .disposition(AgentDisposition.builder()
                        .dispositionProfile(MbtiTypeTerm.INTP.defaultProfile())
                        .build())
                .build();
    }

    // ── Vocabulary Discovery ────────────────────────────────────────────────────

    @Test
    void jungian_vocabulary_registered_via_cdi() {
        assertThat(vocabRegistry.isRegistered(JungianFunctionTerm.URI)).isTrue();
    }

    @Test
    void mbti_vocabulary_registered_via_cdi() {
        assertThat(vocabRegistry.isRegistered(MbtiTypeTerm.URI)).isTrue();
    }

    @Test
    void intp_specializes_ti_and_ne() {
        assertThat(MbtiTypeTerm.INTP.specializes())
                .containsExactly(JungianFunctionTerm.TI, JungianFunctionTerm.NE);
    }

    @Test
    void ti_shadow_is_te() {
        assertThat(JungianFunctionTerm.TI.shadow()).isEqualTo(JungianFunctionTerm.TE);
    }

    // ── Cross-Vocabulary Projection ─────────────────────────────────────────────

    @Test
    void ti_resolves_to_independent_on_social_orient() {
        var result = vocabRegistry.equivalentValues(
                JungianFunctionTerm.URI, "ti",
                "urn:casehub:vocab:conscientiousness",
                DispositionAxis.SOCIAL_ORIENTATION);
        assertThat(result).contains("independent");
    }

    @Test
    void fe_resolves_to_collaborating_on_conflict_mode() {
        var result = vocabRegistry.equivalentValues(
                JungianFunctionTerm.URI, "fe",
                "urn:casehub:vocab:thomas-kilmann",
                DispositionAxis.CONFLICT_MODE);
        assertThat(result).contains("collaborating");
    }

    // ── Weighted Disposition Profile ────────────────────────────────────────────

    @Test
    void descriptor_with_jungian_profile_registered() {
        var descriptor = intpAnalyst();
        registry.register(descriptor);

        var found = registry.findById("intp-agent", TENANCY);
        assertThat(found).isPresent();
        assertThat(found.get().disposition().dispositionProfile()).hasSize(8);
    }

    @Test
    void default_profile_weights_sum_to_one() {
        var profile = MbtiTypeTerm.INTP.defaultProfile();
        double sum = profile.stream().mapToDouble(DispositionValue::weight).sum();
        assertThat(sum).isCloseTo(1.0, within(0.001));
    }

    @Test
    void default_profile_dominant_has_highest_weight() {
        var profile = MbtiTypeTerm.INTP.defaultProfile();
        var dominant = profile.stream()
                .max(Comparator.comparingDouble(DispositionValue::weight))
                .orElseThrow();
        assertThat(dominant.term()).isEqualTo("ti");
        assertThat(dominant.weight()).isEqualTo(0.35);
    }

    // ── Rendering ───────────────────────────────────────────────────────────────

    @Test
    void markdown_renders_cognitive_style_section() {
        registry.register(intpAnalyst());
        var descriptor = registry.findById("intp-agent", TENANCY).orElseThrow();
        var result = renderer.render(descriptor, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        assertThat(result.content()).contains("## Cognitive Style");
        assertThat(result.content()).contains("Dominant");
        assertThat(result.content()).contains("Introverted Thinking");
        assertThat(result.content()).contains("Auxiliary");
        assertThat(result.content()).contains("Extraverted Intuition");
    }

    @Test
    void markdown_renders_weighted_axes_with_primarily_pattern() {
        var descriptor = AgentDescriptor.builder()
                .agentId("weighted-agent").name("Weighted Agent")
                .slot("analyst").tenancyId(TENANCY)
                .dispositionVocabulary("urn:casehub:vocab:conscientiousness")
                .disposition(AgentDisposition.builder()
                        .socialOrient(List.of(
                                new DispositionValue("independent", 0.7),
                                new DispositionValue("collaborative", 0.3)))
                        .build())
                .build();
        registry.register(descriptor);

        var found = registry.findById("weighted-agent", TENANCY).orElseThrow();
        var result = renderer.render(found, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        assertThat(result.content()).contains("primarily");
        assertThat(result.content()).contains("0.7");
        assertThat(result.content()).contains("tendencies");
    }

    @Test
    void a2a_card_includes_disposition_profile() throws Exception {
        registry.register(intpAnalyst());
        var descriptor = registry.findById("intp-agent", TENANCY).orElseThrow();
        var result     = renderer.render(descriptor, AgentPromptContext.forFormat(RenderFormat.A2A_CARD));

        var card    = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.content());
        var profile = card.get("dispositionProfile");
        assertThat(profile).isNotNull();
        var functions = profile.get("functions");
        assertThat(functions.isArray()).isTrue();
        assertThat(functions.size()).isEqualTo(8);
        assertThat(profile.get("vocabulary").asText()).isEqualTo(JungianFunctionTerm.URI);
        assertThat(profile.has("derivedMbtiType")).isTrue();
    }

    // ── Disposition Health Lifecycle ─────────────────────────────────────────────

    @Test
    void probe_aligned_when_no_signals() {
        var descriptor = intpAnalyst();
        var status = dispositionHealth.probe(descriptor, CapabilityHealth.ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.Aligned.class);
    }

    @Test
    void probe_drifted_after_small_activation() {
        var descriptor = intpAnalyst();
        signalStore.recordActivation("intp-agent", TENANCY, "ne");

        var status = dispositionHealth.probe(descriptor, CapabilityHealth.ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.Drifted.class);
        var drifted = (DispositionStatus.Drifted) status;
        assertThat(drifted.mostActivated()).isEqualTo("ne");
        assertThat(drifted.driftMagnitude()).isGreaterThan(0.0);
    }

    @Test
    void probe_evolution_pending_after_strong_auxiliary_activation() {
        var descriptor = intpAnalyst();
        // ne (auxiliary, base 0.20) needs effective weight >= ti base (0.35)
        for (int i = 0; i < 4; i++) {
            signalStore.recordActivation("intp-agent", TENANCY, "ne");
        }

        var status = dispositionHealth.probe(descriptor, CapabilityHealth.ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.EvolutionPending.class);
        var pending = (DispositionStatus.EvolutionPending) status;
        assertThat(pending.type().name()).isEqualTo("DOMINANT_AUXILIARY_SWAP");
        assertThat(pending.candidateFunction()).isEqualTo("ne");
    }

    @Test
    void evolution_produces_new_profile_with_swapped_dominant() {
        var descriptor = intpAnalyst();
        for (int i = 0; i < 4; i++) {
            signalStore.recordActivation("intp-agent", TENANCY, "ne");
        }

        var status = dispositionHealth.probe(descriptor, CapabilityHealth.ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.EvolutionPending.class);
        var pending = (DispositionStatus.EvolutionPending) status;

        var result = dispositionEvolution.evaluate(descriptor, pending);
        assertThat(result).isInstanceOf(EvolutionResult.Evolved.class);

        var evolved = (EvolutionResult.Evolved) result;
        assertThat(evolved.newProfile()).isNotEmpty();
        double sum = evolved.newProfile().stream()
                .mapToDouble(DispositionValue::weight).sum();
        assertThat(sum).isCloseTo(1.0, within(0.001));

        var newDominant = evolved.newProfile().stream()
                .max(Comparator.comparingDouble(DispositionValue::weight))
                .orElseThrow();
        assertThat(newDominant.term()).isEqualTo("ne");
        assertThat(evolved.previousTypeLabel()).isNotEqualTo(evolved.newTypeLabel());
    }
}
