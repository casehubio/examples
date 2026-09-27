package io.casehub.examples.manor.agent;

import io.casehub.blocks.agentic.social.CognitionConfig;
import io.casehub.blocks.agentic.social.prompt.CognitiveSystemPromptRenderer;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentVoiceProfile;
import io.casehub.eidos.api.SystemPromptRenderer;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VoiceProfileRendererTest {

    @Test void rendersVoiceDescriptionAsPrimarySignal() {
        var voice = new AgentVoiceProfile(
                "Penelope Pitstop's signature Southern belle",
                "southern-belle", "southern-drawl",
                List.of("Why, how delightful!"),
                List.of("warm and effusive"),
                List.of("simply"), List.of(),
                List.of("exclaims when discovering"), null);
        var desc = AgentDescriptor.builder()
                .agentId("test").name("Penelope Pitstop").slot("s").tenancyId("t")
                .briefing("A glamorous Southern belle.")
                .voice(voice)
                .build();
        var renderer = new CognitiveSystemPromptRenderer(CognitionConfig.all());
        var result = renderer.render(desc, AgentPromptContext.forFormat(
                SystemPromptRenderer.RenderFormat.MARKDOWN));
        assertThat(result.content()).contains("## Voice");
        assertThat(result.content()).contains("Penelope Pitstop's signature Southern belle");
        assertThat(result.content()).contains("southern-belle");
        assertThat(result.content()).contains("Why, how delightful!");
        assertThat(result.content()).contains("A glamorous Southern belle.");
    }

    @Test void rendersPersonasWithDescriptions() {
        var sneekly = new AgentVoiceProfile(
                "Unctuous Sylvester Sneekly",
                "obsequious", null,
                null, null, null, null, null, null);
        var claw = new AgentVoiceProfile(
                "Grandiose theatrical villain",
                "grandiose", null,
                null, null, null, null, null, null);
        var personas = new LinkedHashMap<String, AgentVoiceProfile>();
        personas.put("sneekly", sneekly);
        personas.put("claw", claw);
        var voice = new AgentVoiceProfile(
                "Dual-persona villain", null, null, null, null, null, null,
                List.of("explains schemes"),
                personas);
        var desc = AgentDescriptor.builder()
                .agentId("test").name("Hooded Claw").slot("s").tenancyId("t")
                .briefing("Secret nemesis.")
                .voice(voice)
                .build();
        var renderer = new CognitiveSystemPromptRenderer(CognitionConfig.all());
        var result = renderer.render(desc, AgentPromptContext.forFormat(
                SystemPromptRenderer.RenderFormat.MARKDOWN));
        assertThat(result.content()).contains("Voice: sneekly");
        assertThat(result.content()).contains("Voice: claw");
        assertThat(result.content()).contains("Unctuous Sylvester Sneekly");
        assertThat(result.content()).contains("Grandiose theatrical villain");
        assertThat(result.content()).contains("explains schemes");
    }

    @Test void fallsBackToBriefingWhenNoVoice() {
        var desc = AgentDescriptor.builder()
                .agentId("test").name("Agent").slot("s").tenancyId("t")
                .briefing("You are a helpful assistant.")
                .build();
        var renderer = new CognitiveSystemPromptRenderer(CognitionConfig.all());
        var result = renderer.render(desc, AgentPromptContext.forFormat(
                SystemPromptRenderer.RenderFormat.MARKDOWN));
        assertThat(result.content()).contains("helpful assistant");
        assertThat(result.content()).doesNotContain("## Voice");
    }

    @Test void includesPreambleAndConstraints() {
        var voice = new AgentVoiceProfile(
                null, "formal", null, null, null, null, null, null, null);
        var hard = new io.casehub.eidos.api.AgentConstraint("no-break", "Never break cover",
                io.casehub.eidos.api.Visibility.PUBLIC, io.casehub.eidos.api.ConstraintSeverity.HARD);
        var soft = new io.casehub.eidos.api.AgentConstraint("be-polite", "Always be polite",
                io.casehub.eidos.api.Visibility.PUBLIC, io.casehub.eidos.api.ConstraintSeverity.SOFT);
        var desc = AgentDescriptor.builder()
                .agentId("test").name("Agent").slot("s").tenancyId("t")
                .voice(voice)
                .constraints(List.of(hard, soft))
                .build();
        var renderer = new CognitiveSystemPromptRenderer(CognitionConfig.all());
        var result = renderer.render(desc, AgentPromptContext.forFormat(
                SystemPromptRenderer.RenderFormat.MARKDOWN));
        assertThat(result.content()).contains("Never break cover");
        assertThat(result.content()).doesNotContain("Always be polite");
        assertThat(result.content()).contains("inner life");
    }
}
