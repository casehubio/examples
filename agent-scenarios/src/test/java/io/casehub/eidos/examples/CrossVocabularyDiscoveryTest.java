package io.casehub.eidos.examples;

import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.vocab.CasehubSlotTerm;
import io.casehub.eidos.vocab.SvoTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class CrossVocabularyDiscoveryTest {

    @Inject VocabularyRegistry vocabRegistry;

    @Test
    void svo_and_casehub_slot_vocabularies_are_registered() {
        assertThat(vocabRegistry.isRegistered(SvoTerm.URI)).isTrue();
        assertThat(vocabRegistry.isRegistered(CasehubSlotTerm.URI)).isTrue();
    }

    @Test
    void typed_svo_evaluator_maps_to_casehub_slot_reviewer() {
        var result = vocabRegistry.equivalentValues(SvoTerm.EVALUATOR, CasehubSlotTerm.class);
        assertThat(result).contains(CasehubSlotTerm.REVIEWER);
    }

    @Test
    void typed_casehub_slot_reviewer_maps_to_svo_evaluator() {
        var result = vocabRegistry.equivalentValues(CasehubSlotTerm.REVIEWER, SvoTerm.class);
        assertThat(result).contains(SvoTerm.EVALUATOR);
    }

    @Test
    void string_svo_evaluator_maps_to_casehub_slot_reviewer() {
        var result = vocabRegistry.equivalentValues(SvoTerm.URI, "evaluator", CasehubSlotTerm.URI);
        assertThat(result).contains("reviewer");
    }

    @Test
    void string_casehub_slot_reviewer_maps_to_svo_evaluator() {
        var result = vocabRegistry.equivalentValues(CasehubSlotTerm.URI, "reviewer", SvoTerm.URI);
        assertThat(result).contains("evaluator");
    }

    @Test
    void cross_reference_is_bidirectional_for_all_pairs() {
        assertThat(vocabRegistry.equivalentValues(SvoTerm.COORDINATOR, CasehubSlotTerm.class))
            .contains(CasehubSlotTerm.PLANNER);
        assertThat(vocabRegistry.equivalentValues(CasehubSlotTerm.PLANNER, SvoTerm.class))
            .contains(SvoTerm.COORDINATOR);
        assertThat(vocabRegistry.equivalentValues(SvoTerm.PERFORMER, CasehubSlotTerm.class))
            .contains(CasehubSlotTerm.EXECUTOR);
        assertThat(vocabRegistry.equivalentValues(CasehubSlotTerm.EXECUTOR, SvoTerm.class))
            .contains(SvoTerm.PERFORMER);
    }

    @Test
    void resolve_term_by_alias_via_registry() {
        // "reviewer" is an alias for SvoTerm.EVALUATOR
        var term = vocabRegistry.resolve(SvoTerm.URI, "reviewer");
        assertThat(term).isPresent();
        assertThat(term.get().value()).isEqualTo("evaluator");
    }

    @Test
    void supervisor_has_no_svo_equivalent() {
        var result = vocabRegistry.equivalentValues(
            CasehubSlotTerm.URI, "supervisor", SvoTerm.URI);
        assertThat(result).isEmpty();
    }
}
