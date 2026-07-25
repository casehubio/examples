package io.casehub.examples.manor.voice;

import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentQuery;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.examples.manor.ManorConstants;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DescriptorLoadTest {

    @Inject
    AgentRegistry registry;

    @Inject
    SystemPromptRenderer renderer;

    @Test
    void five_characters_registered_at_startup() {
        var all = registry.find(AgentQuery.all(ManorConstants.TENANCY_ID));
        assertThat(all).hasSize(5);
        assertThat(all).extracting(m -> m.descriptor().agentId())
                       .containsExactlyInAnyOrder(
                               "penelope-pitstop", "hooded-claw", "ant-hill-mob",
                               "dick-dastardly", "peter-perfect");
    }

    @Test
    void hooded_claw_has_villain_disposition() {
        var desc = registry.findById("hooded-claw", ManorConstants.TENANCY_ID).orElseThrow();
        assertThat(desc.disposition().riskAppetite()).isEqualTo("extreme");
        assertThat(desc.disposition().conflictMode()).isEqualTo("competing");
        assertThat(desc.briefing()).containsIgnoringCase("Hooded Claw");
        assertThat(desc.templates()).extracting(t -> t.templateId())
                                    .contains("cartoon-villain");}

    @Test
    void penelope_has_collaborative_disposition() {
        var desc = registry.findById("penelope-pitstop", ManorConstants.TENANCY_ID).orElseThrow();
        assertThat(desc.disposition().socialOrient()).isEqualTo("collaborative");
        assertThat(desc.briefing()).containsIgnoringCase("Southern");
    }

    @Test
    void hooded_claw_has_villain_template() {
        var desc = registry.findById("hooded-claw", ManorConstants.TENANCY_ID).orElseThrow();
        assertThat(desc.templates()).isNotEmpty();
        assertThat(desc.templates()).extracting(t -> t.templateId())
                                    .contains("hanna-barbera-cartoon-style", "cartoon-villain");
    }

    @Test
    void rendered_prompt_includes_template_content() {
        var desc     = registry.findById("hooded-claw", ManorConstants.TENANCY_ID).orElseThrow();
        var ctx      = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        var rendered = renderer.render(desc, ctx);
        assertThat(rendered.content())
                .as("Rendered prompt should include template content (expository soliloquy)")
                .containsIgnoringCase("expository soliloquy")
                .containsIgnoringCase("emotional telegraphing");
        assertThat(rendered.content())
                .as("Rendered prompt should include villain template with substituted args")
                .containsIgnoringCase("Nyah-ha-ha-HA!")
                .containsIgnoringCase("Penelope Pitstop");
    }

    @Test
    void all_characters_share_hanna_barbera_template() {
        var all = registry.find(AgentQuery.all(ManorConstants.TENANCY_ID));
        for (var match : all) {
            assertThat(match.descriptor().templates())
                    .as("Character %s should reference hanna-barbera-cartoon-style", match.descriptor().name())
                    .extracting(t -> t.templateId())
                    .contains("hanna-barbera-cartoon-style");
        }
    }
}
