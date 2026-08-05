package io.casehub.examples.manor.experiment;

import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionFormatConstraintTest {

    private final AgentDisposition teDisposition = AgentDisposition.builder()
            .dispositionProfile(
                    new DispositionValue("te", 0.40),
                    new DispositionValue("ni", 0.30))
            .build();

    private final AgentDisposition noProfile = AgentDisposition.builder()
            .socialOrient("collaborative")
            .build();

    @Test
    void forDominant_returns_te_constraint() {
        assertThat(FunctionFormatConstraint.forDominant("te"))
                .contains("numbered action plans");
    }

    @Test
    void forDominant_returns_null_for_unknown() {
        assertThat(FunctionFormatConstraint.forDominant("xx")).isNull();
    }

    @Test
    void cognitiveApproach_returns_directive_for_te() {
        String result = FunctionFormatConstraint.cognitiveApproach(teDisposition);
        assertThat(result).contains("systematic");
    }

    @Test
    void cognitiveApproach_returns_null_for_no_profile() {
        assertThat(FunctionFormatConstraint.cognitiveApproach(noProfile)).isNull();
    }

    @Test
    void reasoningInstruction_returns_text_for_te() {
        String result = FunctionFormatConstraint.reasoningInstruction(teDisposition);
        assertThat(result).contains("systematic analysis");
    }

    @Test
    void thinkingDescription_returns_parameterized_for_te() {
        String result = FunctionFormatConstraint.thinkingDescription(teDisposition);
        assertThat(result).contains("systematic analysis");
    }

    @Test
    void all_eight_functions_have_format_constraints() {
        for (String fn : List.of("te", "ti", "fe", "fi", "se", "si", "ni", "ne")) {
            assertThat(FunctionFormatConstraint.forDominant(fn))
                    .as("Missing format constraint for %s", fn)
                    .isNotNull();
        }
    }
}
