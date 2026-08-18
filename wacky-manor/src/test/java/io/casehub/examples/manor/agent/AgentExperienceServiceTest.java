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

    private CaseMemoryStore capturingStore(List<Memory> memories, List<MemoryQuery> captured) {
        return new CaseMemoryStore() {
            @Override
            public String store(MemoryInput input) {return "id";}

            @Override
            public List<Memory> query(MemoryQuery q) {
                                                       captured.add(q);
                                                       return memories;
                                                   }

            @Override
            public int erase(EraseRequest r) {return 0;}
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

    @Test
    void recallUsesSalienceOrder() {
        var captured = new ArrayList<MemoryQuery>();
        var service  = new AgentExperienceService(stubRecorder(), capturingStore(List.of(), captured), "t1");

        service.recall("agent-1", 20);

        assertThat(captured).hasSize(1);
        assertThat(captured.getFirst().order()).isEqualTo(io.casehub.neocortex.memory.MemoryOrder.SALIENCE);
    }

    @Test
    void ingestPassesImportanceToAction() {
        var service = new AgentExperienceService(stubRecorder(), stubStore(List.of()), "t1");

        service.ingest("agent-1", "kitchen", "took the poison", null, 0.8);

        assertThat(recorded).hasSize(1);
        assertThat(recorded.getFirst().importance()).isEqualTo(0.8);
    }

    @Test
    void ingestSetsTargetAgentMetadata() {
        var service = new AgentExperienceService(stubRecorder(), stubStore(List.of()), "t1");

        service.ingest("agent-1", "kitchen", "gave poison to penelope",
                       null, 0.7, "penelope");

        assertThat(recorded).hasSize(1);
        assertThat(recorded.getFirst().metadata())
                .containsEntry(io.casehub.neocortex.memory.experience.ExperienceAttributeKeys.TARGET_AGENT, "penelope");
    }

    @Test
    void ingestOmitsTargetAgentWhenNull() {
        var service = new AgentExperienceService(stubRecorder(), stubStore(List.of()), "t1");

        service.ingest("agent-1", "kitchen", "looked around", null, 0.2, null);

        assertThat(recorded.getFirst().metadata())
                .doesNotContainKey(io.casehub.neocortex.memory.experience.ExperienceAttributeKeys.TARGET_AGENT);
    }

    @Test
    void recallRelationshipsQueriesRelationshipDomain() {
        var captured = new ArrayList<MemoryQuery>();
        var service  = new AgentExperienceService(stubRecorder(), capturingStore(List.of(), captured), "t1");

        service.recallRelationships("agent-1", "agent-2", 3);

        assertThat(captured).hasSize(1);
        assertThat(captured.getFirst().order()).isEqualTo(io.casehub.neocortex.memory.MemoryOrder.SALIENCE);
    }

    @Test
    void ingestTriggersReflectionAtThreshold() throws Exception {
        var storedInputs = new ArrayList<MemoryInput>();
        CaseMemoryStore reflectStore = new CaseMemoryStore() {
            @Override
            public String store(MemoryInput input) {
                storedInputs.add(input);
                return "r-" + storedInputs.size();
            }

            @Override
            public List<Memory> query(MemoryQuery q) {
                return List.of(new Memory("m1", "a1", new MemoryDomain("manor"), "t1",
                                          null, "test memory", Map.of(), Instant.now(), 0.5));
            }

            @Override
            public int erase(EraseRequest r) {return 0;}
        };
        var synthesizer = new io.casehub.neocortex.memory.reflection.ReflectionSynthesizer() {
            final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();

            @Override
            public List<io.casehub.neocortex.memory.reflection.ReflectionEvent> synthesize(
                    String agentId, String tenantId, List<Memory> sources, int targetLevel) {
                calls.incrementAndGet();
                return List.of(new io.casehub.neocortex.memory.reflection.ReflectionEvent(
                        agentId, tenantId, null, "test insight", 1, List.of("m1"), 0.8, Map.of()));
            }
        };
        var trigger = new ManorReflectionTrigger(2, 100.0);
        var service = new AgentExperienceService(stubRecorder(), reflectStore, "t1",
                                                 synthesizer, trigger, true, false, 7, 0.2, 15, 20);

        service.ingest("a1", "room", "action 1", null, 0.5, null);
        service.ingest("a1", "room", "action 2", null, 0.5, null);

        Thread.sleep(1000);

        assertThat(synthesizer.calls.get()).isEqualTo(1);
        assertThat(storedInputs).isNotEmpty();
    }

    @Test
    void recallReflectionsQueriesReflectionDomain() {
        var captured = new ArrayList<MemoryQuery>();
        var service  = new AgentExperienceService(stubRecorder(), capturingStore(List.of(), captured), "t1");

        service.recallReflections("a1", 5);

        assertThat(captured).hasSize(1);
        assertThat(captured.getFirst().domain())
                .isEqualTo(io.casehub.neocortex.memory.reflection.ReflectionEvents.DOMAIN);
        assertThat(captured.getFirst().order()).isEqualTo(io.casehub.neocortex.memory.MemoryOrder.SALIENCE);
    }

    @Test
    void reflectionChainsGoalEvaluation() throws Exception {
        var storedInputs = new ArrayList<MemoryInput>();
        CaseMemoryStore reflectStore = new CaseMemoryStore() {
            @Override
            public String store(MemoryInput input) {
                storedInputs.add(input);
                return "r-" + storedInputs.size();
            }

            @Override
            public List<Memory> query(MemoryQuery q) {
                return List.of(new Memory("m1", "a1", new MemoryDomain("manor"), "t1",
                                          null, "test memory", Map.of(), Instant.now(), 0.5));
            }

            @Override
            public int erase(EraseRequest r) {return 0;}
        };
        var synthesizer = new io.casehub.neocortex.memory.reflection.ReflectionSynthesizer() {
            @Override
            public List<io.casehub.neocortex.memory.reflection.ReflectionEvent> synthesize(
                    String agentId, String tenantId, List<Memory> sources, int targetLevel) {
                return List.of(new io.casehub.neocortex.memory.reflection.ReflectionEvent(
                        agentId, tenantId, null, "test insight", 1, List.of("m1"), 0.8, Map.of()));
            }
        };
        var trigger = new ManorReflectionTrigger(2, 100.0);

        var evaluatedInsights = new ArrayList<List<String>>();
        var evaluatedTicks    = new ArrayList<Integer>();
        ManorGoalEvaluator mockEvaluator = new ManorGoalEvaluator(
                ctx -> new io.casehub.api.spi.routing.GoalFormationProposal(List.of(), ""),
                null, null, reflectStore, "t1", 10, 2) {
            @Override
            public void evaluate(String agentId, int currentTick, List<String> insights,
                                 Map<String, io.casehub.eidos.api.GoalOutcomeCounts> goalOutcomes) {
                evaluatedInsights.add(insights);
                evaluatedTicks.add(currentTick);
            }
        };

        var service = new AgentExperienceService(stubRecorder(), reflectStore, "t1",
                                                 synthesizer, trigger, true, false, 7, 0.2, 15, 20,
                                                 mockEvaluator);

        service.ingest("a1", "room", "action 1", null, 0.5, null, 5);
        service.ingest("a1", "room", "action 2", null, 0.5, null, 5);

        Thread.sleep(1000);

        assertThat(evaluatedInsights).hasSize(1);
        assertThat(evaluatedInsights.getFirst()).containsExactly("test insight");
        assertThat(evaluatedTicks.getFirst()).isEqualTo(5);
    }


}
