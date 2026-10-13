package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormationMemoryParserTest {

    @Test
    void extractAnnotations_singleInline() {
        var text = "You built a trap. {{serves: scheming|strong: The pieces came together exactly as planned.}} Nobody saw it.";
        var annotations = FormationMemoryParser.extractAnnotations(text);

        assertThat(annotations).hasSize(1);
        assertThat(annotations.get(0).drive()).isEqualTo("scheming");
        assertThat(annotations.get(0).degree()).isEqualTo("strong");
        assertThat(annotations.get(0).text()).isEqualTo("The pieces came together exactly as planned.");
        assertThat(annotations.get(0).startOffset()).isEqualTo(text.indexOf("{{"));
        assertThat(annotations.get(0).endOffset()).isEqualTo(text.indexOf("}}") + 2);
    }

    @Test
    void extractAnnotations_multipleInline() {
        var text = "{{serves: scheming|strong: The settling in your chest.}} Nobody saw. {{serves: recognition|strong: It felt better with an audience.}}";
        var annotations = FormationMemoryParser.extractAnnotations(text);

        assertThat(annotations).hasSize(2);
        assertThat(annotations.get(0).drive()).isEqualTo("scheming");
        assertThat(annotations.get(1).drive()).isEqualTo("recognition");
        assertThat(annotations.get(1).degree()).isEqualTo("strong");
    }

    @Test
    void extractAnnotations_hyphenatedDrive() {
        var text = "{{serves: social-harmony|moderate: The tightness dissolved.}}";
        var annotations = FormationMemoryParser.extractAnnotations(text);

        assertThat(annotations).hasSize(1);
        assertThat(annotations.get(0).drive()).isEqualTo("social-harmony");
    }

    @Test
    void extractAnnotations_noAnnotations() {
        var text = "Plain episode text with no annotations.";
        assertThat(FormationMemoryParser.extractAnnotations(text)).isEmpty();
    }

    @Test
    void stripAnnotations_replacesWithText() {
        var text = "You built a trap. {{serves: scheming|strong: The pieces came together.}} Nobody saw.";
        var stripped = FormationMemoryParser.stripAnnotations(text);

        assertThat(stripped).isEqualTo("You built a trap. The pieces came together. Nobody saw.");
        assertThat(stripped).doesNotContain("{{");
        assertThat(stripped).doesNotContain("}}");
        assertThat(stripped).doesNotContain("serves:");
    }

    @Test
    void stripAnnotations_multipleAnnotations() {
        var text = "{{serves: scheming|strong: Settling.}} Gap. {{serves: recognition|strong: Audience.}}";
        var stripped = FormationMemoryParser.stripAnnotations(text);

        assertThat(stripped).isEqualTo("Settling. Gap. Audience.");
    }

    @Test
    void stripAnnotations_noAnnotations_returnsOriginal() {
        var text = "Plain text.";
        assertThat(FormationMemoryParser.stripAnnotations(text)).isEqualTo("Plain text.");
    }

    @Test
    void parseWholeMemory_validFormat() {
        var annotation = FormationMemoryParser.parseWholeMemory("scheming|defining", "The first time you hid a bread roll.");

        assertThat(annotation.drive()).isEqualTo("scheming");
        assertThat(annotation.degree()).isEqualTo("defining");
        assertThat(annotation.text()).isEqualTo("The first time you hid a bread roll.");
        assertThat(annotation.startOffset()).isEqualTo(0);
    }

    @Test
    void parseWholeMemory_invalidFormat_throws() {
        assertThatThrownBy(() -> FormationMemoryParser.parseWholeMemory("scheming", "text"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("drive|degree");
    }

    @Test
    void roundtrip_extractThenStrip() {
        var original = "You built a trap — {{serves: scheming|strong: a system of strings and counterweights.}} Nobody else saw it. {{serves: recognition|strong: It felt so much better when there was someone to show it to — the architecture deserved an audience.}}";

        var annotations = FormationMemoryParser.extractAnnotations(original);
        var stripped = FormationMemoryParser.stripAnnotations(original);

        assertThat(annotations).hasSize(2);
        assertThat(stripped).isEqualTo("You built a trap — a system of strings and counterweights. Nobody else saw it. It felt so much better when there was someone to show it to — the architecture deserved an audience.");
        assertThat(stripped).doesNotContain("serves:");
    }

    @Test
    void loaderIntegration_genericConfigHasAnnotations() {
        var configs = ManorSocialConfigLoader.loadForProfile("GENERIC");
        var hc = configs.get("hooded-claw");
        for (var mem : hc.formationMemories()) {
            assertThat(mem.annotations()).isNotNull();
        }
    }
}
