package io.casehub.eidos.examples;

import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.vocab.ConscientiousnessTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class DispositionVocabularyTest {

    @Inject VocabularyRegistry vocabRegistry;

    @Test
    void conscientiousness_vocabulary_is_registered() {
        assertThat(vocabRegistry.isRegistered(ConscientiousnessTerm.URI)).isTrue();
    }

    @Test
    void typed_resolve_strict_by_primary_value() {
        var result = vocabRegistry.resolve(ConscientiousnessTerm.class, "strict");
        assertThat(result).contains(ConscientiousnessTerm.STRICT);
    }

    @Test
    void typed_resolve_conservative_by_alias() {
        var result = vocabRegistry.resolve(ConscientiousnessTerm.class, "risk-averse");
        assertThat(result).contains(ConscientiousnessTerm.CONSERVATIVE);
    }

    @Test
    void all_terms_returns_12_in_declaration_order() {
        var terms = vocabRegistry.allTerms(ConscientiousnessTerm.URI);
        assertThat(terms).hasSize(12);
        // First term in declaration order is STRICT (rule-following axis)
        assertThat(terms.get(0).value()).isEqualTo("strict");
        // Last term is AUTONOMOUS (autonomy axis)
        assertThat(terms.get(11).value()).isEqualTo("autonomous");
    }

    @Test
    void resolve_rule_following_axis_values() {
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "strict")).isPresent();
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "principled")).isPresent();
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "flexible")).isPresent();

        var strict = vocabRegistry.resolve(ConscientiousnessTerm.class, "strict").get();
        assertThat(strict.label()).isEqualTo("Strict Rule Following");
        assertThat(strict.aliases()).contains("rule-bound");
    }

    @Test
    void resolve_risk_appetite_axis_values() {
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "conservative")).isPresent();
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "measured")).isPresent();
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "bold")).isPresent();

        var bold = vocabRegistry.resolve(ConscientiousnessTerm.class, "bold").get();
        assertThat(bold.aliases()).contains("risk-tolerant");
    }

    @Test
    void resolve_social_orientation_axis_values() {
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "collaborative")).isPresent();
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "independent")).isPresent();
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "facilitative")).isPresent();
    }

    @Test
    void resolve_autonomy_axis_values() {
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "directed")).isPresent();
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "semi-autonomous")).isPresent();
        assertThat(vocabRegistry.resolve(ConscientiousnessTerm.class, "autonomous")).isPresent();

        var autonomous = vocabRegistry.resolve(ConscientiousnessTerm.class, "autonomous").get();
        assertThat(autonomous.aliases()).contains("self-governing", "agentic");
    }

    @Test
    void resolve_by_alias() {
        var term = vocabRegistry.resolve(ConscientiousnessTerm.URI, "risk-averse");
        assertThat(term).isPresent();
        assertThat(term.get().value()).isEqualTo("conservative");
    }
}
