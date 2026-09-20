package io.casehub.examples.manor.agent;

import io.casehub.blocks.agentic.social.MentalModelSnapshot;
import io.casehub.blocks.agentic.social.StrategyProfile;
import io.casehub.blocks.agentic.social.UserProfile;
import io.casehub.blocks.agentic.social.narrative.NarrativeScope;
import io.casehub.blocks.agentic.social.narrative.NarrativeState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryStoreTest {

    @Test void narrativeStoreRoundTrip() {
        var store = new InMemoryNarrativeStore();
        var state = new NarrativeState("agent-1", "tenant-1",
                NarrativeScope.INDIVIDUAL, List.of(), Instant.now(), 0);
        store.store(state);
        assertThat(store.load("agent-1", "tenant-1")).isNotNull();
        assertThat(store.load("agent-1", "tenant-1").scopeId()).isEqualTo("agent-1");
        assertThat(store.load("other", "tenant-1")).isNull();
    }

    @Test void userProfileStoreRoundTrip() {
        var store = new InMemoryUserProfileStore();
        var now = Instant.now();
        var profile = new UserProfile("agent-1", "subject-1", "tenant-1",
                "stranger", 0.0, 0, 0, 0, 0, now, now, now,
                null, null, null, null, Map.of());
        store.store(profile);
        assertThat(store.lookup("agent-1", "subject-1", "tenant-1")).isPresent();
        assertThat(store.findByAgent("agent-1", "tenant-1")).hasSize(1);
        assertThat(store.lookup("agent-1", "other", "tenant-1")).isEmpty();
    }

    @Test void mentalModelStoreRoundTrip() {
        var store = new InMemoryMentalModelStore();
        var now = Instant.now();
        var snapshot = new MentalModelSnapshot("agent-1", "subject-1", "tenant-1",
                List.of(), List.of(), List.of(), now, now, now);
        store.store(snapshot);
        assertThat(store.lookup("agent-1", "subject-1", "tenant-1")).isPresent();
        assertThat(store.findByAgent("agent-1", "tenant-1")).hasSize(1);
        assertThat(store.lookup("agent-1", "other", "tenant-1")).isEmpty();
    }

    @Test void strategyStoreRoundTrip() {
        var store = new InMemoryStrategyStore();
        var profile = new StrategyProfile("agent-1", "tenant-1",
                Map.of(), List.of(), Instant.now(), 0);
        store.store(profile);
        assertThat(store.lookup("agent-1", "tenant-1")).isPresent();
        assertThat(store.lookup("other", "tenant-1")).isEmpty();
    }
}
