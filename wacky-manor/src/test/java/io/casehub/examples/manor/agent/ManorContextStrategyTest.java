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
    void selectNormsDelegatesToScoringWithBudget() {
        var strategy = new ManorContextStrategy();
        var norms = List.of(
                new SocialConfig.NormEntry("Never help Penelope directly", 10),
                new SocialConfig.NormEntry("Maintain charm", 5),
                new SocialConfig.NormEntry("Protect schemes", 8));
        var budget = CognitiveBudget.forSituation(0, 0.3, 0);
        var result = strategy.selectNorms(norms, List.of("Penelope Pitstop"), List.of(), budget);
        assertThat(result).hasSize(3);
        assertThat(result.get(0).rule()).isEqualTo("Never help Penelope directly");
    }

    @Test
    void selectNormsRespectsMaxNormsFromBudget() {
        var strategy = new ManorContextStrategy();
        var norms = List.of(
                new SocialConfig.NormEntry("A", 10),
                new SocialConfig.NormEntry("B", 8),
                new SocialConfig.NormEntry("C", 6),
                new SocialConfig.NormEntry("D", 4),
                new SocialConfig.NormEntry("E", 2));
        var budget = new CognitiveBudget(5, 2, 3, 3);
        var result = strategy.selectNorms(norms, List.of(), List.of(), budget);
        assertThat(result).hasSize(3);
    }

    @Test
    void budgetForNoNearbyHasMinimalTrust() {
        var strategy = new ManorContextStrategy();
        var budget = strategy.budgetFor(0, 0.3, 0);
        assertThat(budget.maxTrust()).isEqualTo(1);
    }
}
