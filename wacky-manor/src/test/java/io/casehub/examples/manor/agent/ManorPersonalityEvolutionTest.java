package io.casehub.examples.manor.agent;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.memory.InMemoryDispositionSignalStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManorPersonalityEvolutionTest {

    private static final String TENANT = "test-tenant";
    private static final String AGENT = "dick-dastardly";

    @Test
    void skipsCheckBeforeIntervalElapsed() {
        var signalStore = new InMemoryDispositionSignalStore();
        var evolution = new ManorPersonalityEvolution(signalStore, TENANT, 5);

        signalStore.recordActivation(AGENT, TENANT, DispositionAxis.RISK_APPETITE.name());

        boolean checked = evolution.checkAndEvolve(AGENT, 3);
        assertThat(checked).isFalse();

        // Signals should still be present (not decayed)
        assertThat(signalStore.activationCounts(AGENT, TENANT)).isNotEmpty();
    }

    @Test
    void checksAtIntervalBoundary() {
        var signalStore = new InMemoryDispositionSignalStore();
        var evolution = new ManorPersonalityEvolution(signalStore, TENANT, 5);

        signalStore.recordActivation(AGENT, TENANT, DispositionAxis.RISK_APPETITE.name());
        signalStore.recordActivation(AGENT, TENANT, DispositionAxis.RISK_APPETITE.name());
        signalStore.recordActivation(AGENT, TENANT, DispositionAxis.RISK_APPETITE.name());

        boolean checked = evolution.checkAndEvolve(AGENT, 5);
        assertThat(checked).isTrue();
    }

    @Test
    void decaysSignalsAfterCheck() {
        var signalStore = new InMemoryDispositionSignalStore();
        var evolution = new ManorPersonalityEvolution(signalStore, TENANT, 5);

        signalStore.recordActivation(AGENT, TENANT, DispositionAxis.RISK_APPETITE.name());
        signalStore.recordActivation(AGENT, TENANT, DispositionAxis.RISK_APPETITE.name());

        var countBefore = signalStore.activationCounts(AGENT, TENANT)
                .getOrDefault(DispositionAxis.RISK_APPETITE.name(), 0);
        assertThat(countBefore).isEqualTo(2);

        evolution.checkAndEvolve(AGENT, 5);

        var countAfter = signalStore.activationCounts(AGENT, TENANT)
                .getOrDefault(DispositionAxis.RISK_APPETITE.name(), 0);
        assertThat(countAfter).isLessThan(countBefore);
    }

    @Test
    void noSignalsMeansNoCheck() {
        var signalStore = new InMemoryDispositionSignalStore();
        var evolution = new ManorPersonalityEvolution(signalStore, TENANT, 5);

        boolean checked = evolution.checkAndEvolve(AGENT, 5);
        assertThat(checked).isFalse();
    }
}
