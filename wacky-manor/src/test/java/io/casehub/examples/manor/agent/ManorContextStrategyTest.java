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

    @Test
    void shouldCompareSocially_schemingDriveAboveThreshold() {
        var strategy = new ManorContextStrategy();
        var config = new SocialConfig(
                List.of(new SocialConfig.Drive("scheming", 0.9, "Schemes")),
                List.of(), List.of(), List.of());
        assertThat(strategy.shouldCompareSocially(config, false)).isTrue();
    }

    @Test
    void shouldCompareSocially_suspicionDriveAboveThreshold() {
        var strategy = new ManorContextStrategy();
        var config = new SocialConfig(
                List.of(new SocialConfig.Drive("suspicion", 0.6, "Suspicious")),
                List.of(), List.of(), List.of());
        assertThat(strategy.shouldCompareSocially(config, false)).isTrue();
    }

    @Test
    void shouldCompareSocially_driveBelowThreshold() {
        var strategy = new ManorContextStrategy();
        var config = new SocialConfig(
                List.of(new SocialConfig.Drive("scheming", 0.3, "Mild")),
                List.of(), List.of(), List.of());
        assertThat(strategy.shouldCompareSocially(config, false)).isFalse();
    }

    @Test
    void shouldCompareSocially_noDrives() {
        var strategy = new ManorContextStrategy();
        assertThat(strategy.shouldCompareSocially(SocialConfig.empty(), false)).isFalse();
    }

    @Test
    void shouldCompareSocially_nonSocialDrive() {
        var strategy = new ManorContextStrategy();
        var config = new SocialConfig(
                List.of(new SocialConfig.Drive("curiosity", 0.9, "Curious")),
                List.of(), List.of(), List.of());
        assertThat(strategy.shouldCompareSocially(config, false)).isFalse();
    }

    @Test
    void shouldCompareSocially_pullAsideOverridesDriveGate() {
        var strategy = new ManorContextStrategy();
        assertThat(strategy.shouldCompareSocially(SocialConfig.empty(), true)).isTrue();
    }

    @Test
    void shouldCompareSocially_pullAsideWithLowDrive() {
        var strategy = new ManorContextStrategy();
        var config = new SocialConfig(
                List.of(new SocialConfig.Drive("scheming", 0.1, "Barely")),
                List.of(), List.of(), List.of());
        assertThat(strategy.shouldCompareSocially(config, true)).isTrue();
    }

}
