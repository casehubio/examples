package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CognitiveBudgetTest {

    @Test
    void baselineBudgetForEmptyRoom() {
        var budget = CognitiveBudget.forSituation(0, 0.3, 0);
        assertThat(budget.maxBeliefs()).isEqualTo(5);
        assertThat(budget.maxTrust()).isEqualTo(1);
        assertThat(budget.maxNorms()).isEqualTo(4);
        assertThat(budget.maxPrinciples()).isEqualTo(3);
    }

    @Test
    void highArousalIncreasesBeliefBudget() {
        var calm = CognitiveBudget.forSituation(2, 0.3, 0);
        var alert = CognitiveBudget.forSituation(2, 0.8, 0);
        assertThat(alert.maxBeliefs()).isGreaterThan(calm.maxBeliefs());
    }

    @Test
    void moreNearbyAgentsIncreaseTrustBudget() {
        var few = CognitiveBudget.forSituation(1, 0.3, 0);
        var many = CognitiveBudget.forSituation(5, 0.3, 0);
        assertThat(many.maxTrust()).isGreaterThan(few.maxTrust());
    }

    @Test
    void activeGoalsIncreaseBeliefBudget() {
        var noGoals = CognitiveBudget.forSituation(2, 0.3, 0);
        var withGoals = CognitiveBudget.forSituation(2, 0.3, 3);
        assertThat(withGoals.maxBeliefs()).isGreaterThan(noGoals.maxBeliefs());
    }

    @Test
    void trustBudgetCappedAtSix() {
        var budget = CognitiveBudget.forSituation(10, 0.3, 0);
        assertThat(budget.maxTrust()).isLessThanOrEqualTo(6);
    }
}
