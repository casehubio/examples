package io.casehub.examples.manor;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.runtime.registrar.ClasspathYamlDescriptorRegistrar;
import io.casehub.examples.manor.model.ProfileMode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class PersonalityCompositionVerificationTest {

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject VocabularyRegistry vocabRegistry;

    private Map<String, AgentDescriptor> loadProfile(ProfileMode mode) {
        var registrar = new ProfileAwareDescriptorRegistrar(mode);
        return registrar.descriptors().stream()
                .collect(Collectors.toMap(AgentDescriptor::agentId, Function.identity()));
    }

    private Map<String, AgentDescriptor> loadProfileWithVocab(ProfileMode mode) {
        var resourcePath = String.format("META-INF/eidos/descriptors-%s.yaml", mode.name().toLowerCase());
        var url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        try (var stream = url.openStream()) {
            return new ClasspathYamlDescriptorRegistrar().loadFrom(stream, vocabRegistry).stream()
                    .collect(Collectors.toMap(AgentDescriptor::agentId, Function.identity()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void jungian_hooded_claw_has_8_function_profile() {
        var jungian = loadProfileWithVocab(ProfileMode.JUNGIAN);
        var hc = jungian.get("hooded-claw");
        assertThat(hc.disposition().dispositionProfile()).hasSize(8);
        assertThat(hc.disposition().dispositionProfile().get(0).term()).isEqualTo("te");
        assertThat(hc.disposition().dispositionProfile().get(0).weight()).isEqualTo(0.35);
        assertThat(hc.disposition().dispositionProfile().get(1).term()).isEqualTo("ni");
        assertThat(hc.disposition().dispositionProfile().get(1).weight()).isEqualTo(0.20);
    }

    @Test
    void baseline_hooded_claw_has_no_profile() {
        var baseline = loadProfile(ProfileMode.BASELINE);
        var hc = baseline.get("hooded-claw");
        assertThat(hc.disposition().dispositionProfile()).isEmpty();
        assertThat(hc.disposition().primaryTerm(DispositionAxis.RISK_APPETITE)).isEqualTo("extreme");
    }

    @Test
    void belbin_hooded_claw_has_shaper_slot() {
        var belbin = loadProfile(ProfileMode.BELBIN);
        var hc = belbin.get("hooded-claw");
        assertThat(hc.slot()).isEqualTo("shaper");
        assertThat(hc.slotVocabulary()).isEqualTo("urn:casehub:vocab:belbin");
        assertThat(hc.disposition().dispositionProfile()).isEmpty();
    }

    @Test
    void composite_hooded_claw_has_both() {
        var composite = loadProfileWithVocab(ProfileMode.COMPOSITE);
        var hc = composite.get("hooded-claw");
        assertThat(hc.disposition().dispositionProfile()).hasSize(8);
        assertThat(hc.slot()).isEqualTo("shaper");
        assertThat(hc.slotVocabulary()).isEqualTo("urn:casehub:vocab:belbin");
    }

    @Test
    void render_prompt_comparison() {
        var baseline = loadProfile(ProfileMode.BASELINE);
        var jungian = loadProfileWithVocab(ProfileMode.JUNGIAN);
        var belbin = loadProfile(ProfileMode.BELBIN);
        var composite = loadProfileWithVocab(ProfileMode.COMPOSITE);
        var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);

        var baselinePrompt = renderer.render(baseline.get("hooded-claw"), ctx);
        var jungianPrompt = renderer.render(jungian.get("hooded-claw"), ctx);
        var belbinPrompt = renderer.render(belbin.get("hooded-claw"), ctx);
        var compositePrompt = renderer.render(composite.get("hooded-claw"), ctx);

        System.out.println("=== BASELINE PROMPT (Hooded Claw) ===");
        System.out.println(baselinePrompt.content());
        System.out.println("\n=== JUNGIAN PROMPT (Hooded Claw) ===");
        System.out.println(jungianPrompt.content());
        System.out.println("\n=== BELBIN PROMPT (Hooded Claw) ===");
        System.out.println(belbinPrompt.content());
        System.out.println("\n=== COMPOSITE PROMPT (Hooded Claw) ===");
        System.out.println(compositePrompt.content());

        assertThat(jungianPrompt.content()).isNotEqualTo(baselinePrompt.content());
        assertThat(belbinPrompt.content()).isNotEqualTo(baselinePrompt.content());
        assertThat(compositePrompt.content()).isNotEqualTo(jungianPrompt.content());
    }
}
