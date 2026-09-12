package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManorNormFilterTest {

    @Test
    void sortsByPriorityDescending() {
        var norms = List.of(
                new SocialConfig.NormEntry("low", 1),
                new SocialConfig.NormEntry("high", 10),
                new SocialConfig.NormEntry("mid", 5));
        var filtered = ManorNormFilter.filter(norms, List.of(), List.of());
        assertThat(filtered.get(0).rule()).isEqualTo("high");
        assertThat(filtered.get(1).rule()).isEqualTo("mid");
        assertThat(filtered.get(2).rule()).isEqualTo("low");
    }

    @Test
    void emptyNormsReturnsEmpty() {
        var filtered = ManorNormFilter.filter(List.of(), List.of(), List.of());
        assertThat(filtered).isEmpty();
    }

    @Test
    void preservesAllNorms() {
        var norms = List.of(
                new SocialConfig.NormEntry("a", 5),
                new SocialConfig.NormEntry("b", 5),
                new SocialConfig.NormEntry("c", 5));
        var filtered = ManorNormFilter.filter(norms, List.of("nearby"), List.of("item"));
        assertThat(filtered).hasSize(3);
    }
}
