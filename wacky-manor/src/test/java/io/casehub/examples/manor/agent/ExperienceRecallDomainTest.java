package io.casehub.examples.manor.agent;

import io.casehub.neocortex.memory.Memory;
import io.casehub.neocortex.memory.inmem.InMemoryMemoryStore;
import io.casehub.neocortex.memory.experience.runtime.ExperienceRecorderCore;
import io.casehub.platform.api.identity.CurrentPrincipal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ExperienceRecallDomainTest {

    private static final String TENANT = "wacky-manor";

    private final CurrentPrincipal principal = new CurrentPrincipal() {
        @Override public String actorId()             { return "test"; }
        @Override public Set<String> groups()         { return Set.of(); }
        @Override public String tenancyId()           { return TENANT; }
        @Override public boolean isCrossTenantAdmin() { return false; }
    };

    @Test
    void ingestedExperienceIsRecallable() {
        var store = new InMemoryMemoryStore(principal);
        var recorder = new ExperienceRecorderCore(store, recorded -> {});
        var service = new AgentExperienceService(
            ExperienceConfig.minimal(recorder, store, TENANT));

        service.ingest("clara-bellingham", "library", "Examined the inscription on the wall", "The markings look ancient");

        List<Memory> recalled = service.recall("clara-bellingham", 10);

        assertThat(recalled)
            .as("Ingested experience must be recallable — storage and recall domains must match")
            .isNotEmpty();
        assertThat(recalled.getFirst().text())
            .contains("Examined the inscription");
    }

    @Test
    void multipleIngestsAllRecallable() {
        var store = new InMemoryMemoryStore(principal);
        var recorder = new ExperienceRecorderCore(store, recorded -> {});
        var service = new AgentExperienceService(
            ExperienceConfig.minimal(recorder, store, TENANT));

        service.ingest("clara-bellingham", "library", "Examined the inscription", null);
        service.ingest("clara-bellingham", "kitchen", "Spoke with Marsh", null);
        service.ingest("clara-bellingham", "hallway", "Moved toward the conservatory", null);

        List<Memory> recalled = service.recall("clara-bellingham", 10);

        assertThat(recalled).hasSize(3);
    }
}
