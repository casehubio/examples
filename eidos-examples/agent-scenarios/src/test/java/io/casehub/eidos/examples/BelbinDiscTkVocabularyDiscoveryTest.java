package io.casehub.eidos.examples;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.vocab.BelbinTerm;
import io.casehub.eidos.vocab.ConscientiousnessTerm;
import io.casehub.eidos.vocab.DiscTerm;
import io.casehub.eidos.vocab.ThomasKilmannTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class BelbinDiscTkVocabularyDiscoveryTest {

    @Inject
    VocabularyRegistry registry;

    @Test
    void belbin_vocabulary_registered_via_cdi() {
        assertThat(registry.isRegistered(BelbinTerm.URI)).isTrue();
    }

    @Test
    void thomas_kilmann_vocabulary_registered_via_cdi() {
        assertThat(registry.isRegistered(ThomasKilmannTerm.URI)).isTrue();
    }

    @Test
    void disc_vocabulary_registered_via_cdi() {
        assertThat(registry.isRegistered(DiscTerm.URI)).isTrue();
    }

    @Test
    void belbin_co_ordinator_resolvable_by_value() {
        assertThat(registry.resolve(BelbinTerm.URI, "co-ordinator")).isPresent();
    }

    @Test
    void belbin_plant_resolvable_by_alias() {
        assertThat(registry.resolve(BelbinTerm.URI, "pl")).isPresent();
    }

    @Test
    void tk_competing_resolvable_by_value() {
        assertThat(registry.resolve(ThomasKilmannTerm.URI, "competing")).isPresent();
    }

    @Test
    void tk_avoiding_resolvable_by_alias() {
        assertThat(registry.resolve(ThomasKilmannTerm.URI, "avoidant")).isPresent();
    }

    @Test
    void disc_dominance_resolvable_by_alias() {
        assertThat(registry.resolve(DiscTerm.URI, "D")).isPresent();
    }

    @Test
    void disc_to_conscientiousness_axis_aware_resolution_via_registry() {
        // Full chain: DISC dominance → SOCIAL_ORIENTATION → conscientiousness INDEPENDENT
        var result = registry.equivalentValues(
            DiscTerm.URI, "dominance", ConscientiousnessTerm.URI, DispositionAxis.SOCIAL_ORIENTATION);
        assertThat(result).contains("independent");
    }

    @Test
    void disc_to_tk_conflict_mode_resolution_via_registry() {
        // Full chain: DISC dominance → CONFLICT_MODE → TK COMPETING
        var result = registry.equivalentValues(
            DiscTerm.URI, "dominance", ThomasKilmannTerm.URI, DispositionAxis.CONFLICT_MODE);
        assertThat(result).contains("competing");
    }

    @Test
    void conscientiousness_vocabulary_still_registered_after_all_new_vocabs() {
        // Regression: adding new vocabs doesn't break existing vocab
        assertThat(registry.resolve(ConscientiousnessTerm.URI, "collaborative")).isPresent();
        assertThat(registry.resolve(ConscientiousnessTerm.URI, "independent")).isPresent();
    }
}
