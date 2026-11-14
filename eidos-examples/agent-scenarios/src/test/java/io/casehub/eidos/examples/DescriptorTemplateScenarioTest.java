package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.TemplateRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DescriptorTemplateScenarioTest {

    @Inject AgentRegistry registry;
    @Inject TemplateRegistry templateRegistry;
    @Inject SystemPromptRenderer renderer;

    @Test void templates_loaded_from_classpath() {
        assertThat(templateRegistry.resolve("document-review-conventions")).isPresent();
        assertThat(templateRegistry.resolve("communication-style")).isPresent();
    }

    @Test void communication_style_template_has_parameters() {
        var t = templateRegistry.resolve("communication-style").orElseThrow();
        assertThat(t.parameters()).containsExactly("formality", "feedback_approach");
    }

    @Test void descriptor_with_templates_registered() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse");
        assertThat(desc).isPresent();
        assertThat(desc.get().templates()).isNotNull().hasSize(2);
        assertThat(desc.get().templates().get(0).templateId()).isEqualTo("document-review-conventions");
        assertThat(desc.get().templates().get(1).templateId()).isEqualTo("communication-style");
        assertThat(desc.get().templates().get(1).args()).containsEntry("formality", "professional");
    }

    @Test void rendered_markdown_includes_template_content() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        var rendered = renderer.render(desc, ctx);
        assertThat(rendered.content()).contains("specific line references");
        assertThat(rendered.content()).contains("professional register");
        assertThat(rendered.content()).contains("collaborative approach");
    }

    @Test void rendered_prose_includes_template_content() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        var ctx = AgentPromptContext.forFormat(RenderFormat.PROSE);
        var rendered = renderer.render(desc, ctx);
        assertThat(rendered.content()).contains("specific line references");
        assertThat(rendered.content()).contains("professional register");
    }

    @Test void parameterized_template_substituted() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        var rendered = renderer.render(desc, ctx);
        assertThat(rendered.content()).doesNotContain("${formality}");
        assertThat(rendered.content()).doesNotContain("${feedback_approach}");
    }

    @Test void a2a_card_does_not_include_template_content() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        var ctx = AgentPromptContext.forFormat(RenderFormat.A2A_CARD);
        var rendered = renderer.render(desc, ctx);
        assertThat(rendered.content()).doesNotContain("specific line references");
        assertThat(rendered.content()).doesNotContain("professional register");
    }

    @Test
    void template_order_preserved_in_rendered_output() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        assertThat(desc.templates()).hasSize(2);
        var ctx                   = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        var rendered              = renderer.render(desc, ctx);
        int firstTemplateContent  = rendered.content().indexOf("specific line references");
        int secondTemplateContent = rendered.content().indexOf("professional register");
        assertThat(firstTemplateContent).isGreaterThan(-1);
        assertThat(secondTemplateContent).isGreaterThan(-1);
        assertThat(firstTemplateContent).isLessThan(secondTemplateContent);
    }

    @Test
    void shared_template_substitution_isolated_per_descriptor() {
        var desc = registry.findById("drafthouse-structural-reviewer", "drafthouse").orElseThrow();
        var commStyleRef = desc.templates().stream()
                               .filter(t -> t.templateId().equals("communication-style")).findFirst().orElseThrow();
        assertThat(commStyleRef.args()).containsEntry("formality", "professional");
        assertThat(commStyleRef.args()).containsEntry("feedback_approach", "collaborative");

        var ctx      = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        var rendered = renderer.render(desc, ctx);
        assertThat(rendered.content()).contains("professional register");
        assertThat(rendered.content()).contains("collaborative approach");
        assertThat(rendered.content()).doesNotContain("${formality}");
        assertThat(rendered.content()).doesNotContain("${feedback_approach}");
    }

    @Test
    void yaml_registered_template_has_correct_metadata() {
        var template = templateRegistry.resolve("document-review-conventions").orElseThrow();
        assertThat(template.name()).isEqualTo("Document Review Conventions");
        assertThat(template.parameters()).isEmpty();
        assertThat(template.content()).contains("specific line references");
        assertThat(template.content()).contains("Categorise findings");
    }
}
