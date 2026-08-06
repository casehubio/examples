package io.casehub.examples.manor.agent;

import io.casehub.neocortex.memory.CaseMemoryStore;
import io.casehub.neocortex.memory.EraseRequest;
import io.casehub.neocortex.memory.Memory;
import io.casehub.neocortex.memory.MemoryDomain;
import io.casehub.neocortex.memory.MemoryInput;
import io.casehub.neocortex.memory.MemoryQuery;
import io.casehub.neocortex.memory.experience.ExperienceEvent;
import io.casehub.neocortex.memory.experience.ExperienceRecorder;
import io.casehub.neocortex.memory.experience.ExperienceStoreResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AgentExperienceServiceTest {

    private final List<ExperienceEvent> recorded = new ArrayList<>();

    private ExperienceRecorder stubRecorder() {
        return new ExperienceRecorder() {
            @Override
            public String record(ExperienceEvent event) {
                recorded.add(event);
                return "mem-" + recorded.size();
            }
            @Override
            public ExperienceStoreResult recordAll(List<ExperienceEvent> events) {
                events.forEach(this::record);
                return new ExperienceStoreResult(
                    events.stream().map(e -> "mem-" + recorded.size()).toList(), List.of());
            }
        };
    }

    private CaseMemoryStore stubStore(List<Memory> memories) {
        return new CaseMemoryStore() {
            @Override public String store(MemoryInput input) { return "id"; }
            @Override public List<Memory> query(MemoryQuery q) { return memories; }
            @Override public int erase(EraseRequest r) { return 0; }
        };
    }

    @Test
    void ingestRecordsExperienceEvent() {
        var service = new AgentExperienceService(stubRecorder(), stubStore(List.of()), "test-tenant");
        service.ingest("hooded-claw", "library", "Searched the bookshelf", "Looking for clues");

        assertThat(recorded).hasSize(1);
        var event = recorded.getFirst();
        assertThat(event.agentId()).isEqualTo("hooded-claw");
        assertThat(event.description()).isEqualTo("Searched the bookshelf");
    }

    @Test
    void ingestFailureDoesNotThrow() {
        ExperienceRecorder failingRecorder = new ExperienceRecorder() {
            @Override public String record(ExperienceEvent event) { throw new RuntimeException("store down"); }
            @Override public ExperienceStoreResult recordAll(List<ExperienceEvent> events) { throw new RuntimeException("store down"); }
        };
        var service = new AgentExperienceService(failingRecorder, stubStore(List.of()), "test-tenant");

        assertThatCode(() -> service.ingest("agent", "room", "desc", "think"))
            .doesNotThrowAnyException();
    }

    @Test
    void recallReturnsMemories() {
        var memories = List.of(
            new Memory("m1", "hooded-claw", new MemoryDomain("manor"), "test-tenant",
                null, "Found a secret passage", Map.of(), Instant.now(), 0.8));
        var service = new AgentExperienceService(stubRecorder(), stubStore(memories), "test-tenant");

        List<Memory> result = service.recall("hooded-claw", 5);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().text()).isEqualTo("Found a secret passage");
    }

    @Test
    void recallReturnsEmptyOnTimeout() {
        CaseMemoryStore slowStore = new CaseMemoryStore() {
            @Override public String store(MemoryInput input) { return "id"; }
            @Override public List<Memory> query(MemoryQuery q) {
                try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return List.of();
            }
            @Override public int erase(EraseRequest r) { return 0; }
        };
        var service = new AgentExperienceService(stubRecorder(), slowStore, "test-tenant");
        service.setRecallTimeoutMs(100);

        List<Memory> result = service.recall("agent", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void recallReturnsEmptyOnStoreFailure() {
        CaseMemoryStore failStore = new CaseMemoryStore() {
            @Override public String store(MemoryInput input) { return "id"; }
            @Override public List<Memory> query(MemoryQuery q) { throw new RuntimeException("query failed"); }
            @Override public int erase(EraseRequest r) { return 0; }
        };
        var service = new AgentExperienceService(stubRecorder(), failStore, "test-tenant");

        List<Memory> result = service.recall("agent", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void ingestMetadataIncludesRoom() {
        var service = new AgentExperienceService(stubRecorder(), stubStore(List.of()), "test-tenant");
        service.ingest("agent-1", "kitchen", "Made tea", null);

        assertThat(recorded.getFirst().metadata()).containsEntry("room", "kitchen");
    }
}
