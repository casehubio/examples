package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManorContextStrategyTest {

    @Test
    void budgetAdaptsToSituation() {
        var strategy = new ManorContextStrategy();
        var calm = strategy.budgetFor(2, 0.3, 0);
        var alert = strategy.budgetFor(2, 0.8, 0);
        assertThat(alert.maxBeliefs()).isGreaterThan(calm.maxBeliefs());
    }

    @Test
    void normFilterSortsByPriority() {
        var strategy = new ManorContextStrategy();
        var norms = List.of(
                new SocialConfig.NormEntry("low", 1),
                new SocialConfig.NormEntry("high", 10));
        var filtered = strategy.filterNorms(norms, List.of(), List.of());
        assertThat(filtered.get(0).rule()).isEqualTo("high");
    }

    @Test
    void budgetForNoNearbyHasMinimalTrust() {
        var strategy = new ManorContextStrategy();
        var budget = strategy.budgetFor(0, 0.3, 0);
        assertThat(budget.maxTrust()).isEqualTo(1);
    }
}
