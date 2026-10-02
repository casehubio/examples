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

    @SuppressWarnings("unchecked")
    @Test
    void verifyNoUnexpectedTemplateArgs() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());

        var templatesUrl   = Thread.currentThread().getContextClassLoader().getResource("META-INF/eidos/templates.yaml");
        var templateData   = (Map<String, Object>) mapper.readValue(templatesUrl.openStream(), Map.class);
        var templates      = (java.util.List<Map<String, Object>>) templateData.get("templates");
        var templateParams = new java.util.HashMap<String, java.util.Set<String>>();
        for (var t : templates) {
            var params = t.containsKey("parameters") ? new java.util.HashSet<>((java.util.List<String>) t.get("parameters")) : new java.util.HashSet<String>();
            templateParams.put((String) t.get("id"), params);
        }

        var descriptorsUrl = Thread.currentThread().getContextClassLoader().getResource("META-INF/eidos/descriptors-composite.yaml");
        var descData       = (Map<String, Object>) mapper.readValue(descriptorsUrl.openStream(), Map.class);
        var descriptors    = (java.util.List<Map<String, Object>>) descData.get("descriptors");

        for (var desc : descriptors) {
            String agentId = (String) desc.get("agentId");
            if (!desc.containsKey("templates")) {continue;}
            var templateList = (java.util.List<Map<String, Object>>) desc.get("templates");
            for (var tref : templateList) {
                String refId   = (String) tref.get("ref");
                var    allowed = templateParams.get(refId);
                if (allowed == null || !tref.containsKey("args")) {continue;}
                var args = (Map<String, Object>) tref.get("args");
                for (var argKey : args.keySet()) {
                    org.assertj.core.api.Assertions.assertThat(allowed)
                                                   .as("Descriptor %s, template %s: unexpected arg '%s' (allowed: %s)",
                                                       agentId, refId, argKey, allowed)
                                                   .contains(argKey);
                }
            }
        }
        System.out.println("All template args validated — no unexpected args found.");
    }
}
