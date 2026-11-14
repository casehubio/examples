package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.vocab.ArchetypeTerm;
import io.casehub.eidos.runtime.registrar.ArchetypeDeriver;
import io.casehub.eidos.vocab.MbtiTypeTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ArchetypeScenarioTest {

    @Inject AgentRegistry registry;
    @Inject VocabularyRegistry vocabRegistry;
    @Inject SystemPromptRenderer renderer;

    static final String TENANCY = "archetype-examples";

    @Test
    void explicitArchetypeRendersInMarkdown() {
        var descriptor = AgentDescriptor.builder()
            .agentId("detective-agent").name("Investigation Specialist")
            .slot("analyst").tenancyId(TENANCY)
            .archetype("detective")
            .archetypeAdjectives(List.of("meticulous", "persistent", "evidence-driven"))
            .build();

        registry.register(descriptor);
        var found = registry.findById("detective-agent", TENANCY);
        assertThat(found).isPresent();
        assertThat(found.get().archetype()).isEqualTo("detective");

        var rendered = renderer.render(descriptor, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
        assertThat(rendered.content()).contains("## Personality");
        assertThat(rendered.content()).contains("**Detective**");
        assertThat(rendered.content()).contains("meticulous, persistent, evidence-driven");
        assertThat(rendered.content()).contains("Uncovers truth");
    }

    @Test
    void archetypeAutoDerivesFromMbtiVocabulary() {
        var descriptor = AgentDescriptor.builder()
            .agentId("mbti-derive").name("MBTI Agent")
            .slot("analyst").tenancyId(TENANCY)
            .dispositionVocabulary(MbtiTypeTerm.URI)
            .disposition(AgentDisposition.builder()
                .dispositionProfile(List.of(new DispositionValue("intj", 1.0)))
                .build())
            .build();

        var derived = ArchetypeDeriver.deriveArchetype(descriptor);
        registry.register(derived);
        var found = registry.findById("mbti-derive", TENANCY);
        assertThat(found).isPresent();
        assertThat(found.get().archetype()).isNotNull();

        var term = findTerm(found.get().archetype());
        assertThat(term).isNotNull();
        assertThat(term.family().quadrant())
            .isIn(io.casehub.eidos.vocab.ArchetypeFamily.Quadrant.values());
    }

    @Test
    void archetypeRendersInA2aCard() {
        var descriptor = AgentDescriptor.builder()
            .agentId("a2a-arch").name("A2A Agent")
            .slot("analyst").tenancyId(TENANCY)
            .archetype("mentor")
            .archetypeAdjectives(List.of("wise", "patient"))
            .build();

        var rendered = renderer.render(descriptor, AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
        assertThat(rendered.content()).contains("\"archetype\"");
        assertThat(rendered.content()).contains("\"mentor\"");
        assertThat(rendered.content()).contains("\"Mentor\"");
        assertThat(rendered.content()).contains("\"wise\"");
    }

    @Test
    void archetypeVocabularyRegistered() {
        assertThat(vocabRegistry.isRegistered(ArchetypeTerm.URI)).isTrue();
        assertThat(vocabRegistry.allTerms(ArchetypeTerm.URI)).hasSize(48);
    }

    private static ArchetypeTerm findTerm(String value) {
        for (var t : ArchetypeTerm.values()) {
            if (t.value().equals(value)) return t;
        }
        return null;
    }
}
