package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.runtime.registrar.ArchetypeDeriver;
import io.casehub.eidos.vocab.AvatarCodec;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class AvatarScenarioTest {

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;

    static final String TENANCY = "avatar-examples";

    @Test
    void explicitAvatarPreserved() {
        var d = AgentDescriptor.builder()
            .agentId("explicit-av").name("Explicit").slot("s").tenancyId(TENANCY)
            .archetype("detective")
            .avatar("https://example.com/custom.png")
            .build();
        var derived = ArchetypeDeriver.deriveArchetype(d);
        assertThat(derived.avatar()).isEqualTo("https://example.com/custom.png");
    }

    @Test
    void avatarAutoDerivedFromArchetype() {
        var d = AgentDescriptor.builder()
            .agentId("auto-av").name("Auto").slot("s").tenancyId(TENANCY)
            .archetype("detective")
            .build();
        var derived = ArchetypeDeriver.deriveArchetype(d);
        assertThat(derived.avatar()).isNotNull();
        assertThat(derived.avatar()).startsWith("mythic:P");
        assertThat(AvatarCodec.isExternalUrl(derived.avatar())).isFalse();
    }

    @Test
    void avatarAppearsInA2aCard() {
        var d = AgentDescriptor.builder()
            .agentId("a2a-av").name("A2A").slot("s").tenancyId(TENANCY)
            .avatar("mythic:P1B")
            .build();
        var rendered = renderer.render(d, AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
        assertThat(rendered.content()).contains("\"avatar\"");
        assertThat(rendered.content()).contains("mythic:P1B");
    }

    @Test
    void avatarAbsentFromMarkdown() {
        var d = AgentDescriptor.builder()
            .agentId("md-av").name("MD").slot("s").tenancyId(TENANCY)
            .avatar("mythic:P1B")
            .build();
        var rendered = renderer.render(d, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
        assertThat(rendered.content()).doesNotContain("mythic:P1B");
    }
}
