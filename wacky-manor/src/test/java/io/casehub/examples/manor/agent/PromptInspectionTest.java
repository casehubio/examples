package io.casehub.examples.manor.agent;

import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.neocortex.cognition.core.CognitionConfig;
import io.casehub.neocortex.cognition.prompt.CognitiveSystemPromptRenderer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class PromptInspectionTest {

    @Test
    void inspectPeterPerfectSystemPrompt() throws Exception {
        var url = Thread.currentThread().getContextClassLoader()
                        .getResource("META-INF/eidos/descriptors-composite.yaml");
        var descriptors = new io.casehub.eidos.core.registrar.ClasspathYamlDescriptorRegistrar()
                                  .loadFrom(url.openStream(), null);
        var peter = descriptors.stream()
                               .filter(d -> "peter-perfect".equals(d.agentId()))
                               .findFirst().orElseThrow();

        var renderer = new CognitiveSystemPromptRenderer(CognitionConfig.all());
        var result = renderer.render(peter, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  SYSTEM PROMPT — Peter Perfect");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(result.content());
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Format: " + result.format());
        System.out.println("Enriched: " + result.enriched());
        System.out.println("═══════════════════════════════════════════════════════");
    }

    @Test
    void inspectHoodedClawCognitiveSections() {
        var socialConfigs = ManorSocialConfigLoader.load();
        var hcConfig      = socialConfigs.get("hooded-claw");

        var cognition = new CharacterCognition("hooded-claw", null, null,
                                               hcConfig, List.of());
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState(
                        "hooded-claw", "The Hooded Claw", "entrance-hall", 0.0, List.of()),
                List.of("penelope-pitstop", "peter-perfect"),
                Map.of("penelope-pitstop", "Penelope Pitstop",
                       "peter-perfect", "Peter Perfect"));

        var renderer = new io.casehub.blocks.summarisation.observation.affordance.AffordanceRenderer();
        var rendered = renderer.renderObservation(sections);

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  COGNITIVE SECTIONS — Hooded Claw");
        System.out.println("  (nearby: Penelope Pitstop, Peter Perfect)");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(rendered);
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Total sections: " + sections.size());
        System.out.println("Section headers: " + sections.stream()
                                                         .map(s -> s.header()).toList());
        System.out.println("═══════════════════════════════════════════════════════");
    }

    @Test
    void inspectHoodedClawSystemPrompt() throws Exception {
        var url = Thread.currentThread().getContextClassLoader()
                        .getResource("META-INF/eidos/descriptors-composite.yaml");
        var descriptors = new io.casehub.eidos.core.registrar.ClasspathYamlDescriptorRegistrar()
                                  .loadFrom(url.openStream(), null);
        var hc = descriptors.stream()
                            .filter(d -> "hooded-claw".equals(d.agentId()))
                            .findFirst().orElseThrow();

        var renderer = new CognitiveSystemPromptRenderer(CognitionConfig.all());
        var result = renderer.render(hc, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  SYSTEM PROMPT — Hooded Claw");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(result.content());
        System.out.println("═══════════════════════════════════════════════════════");
    }
}
