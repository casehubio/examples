package io.casehub.examples.manor.agent;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionInit;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AgentInvocationServiceTest {

    private AgentProvider stubProvider(String jsonResponse) {
        return new AgentProvider() {
            @Override public Multi<AgentEvent> invoke(io.casehub.platform.agent.AgentSessionConfig config) {
                return Multi.createFrom().item((AgentEvent) new AgentEvent.TextDelta(jsonResponse));
            }
            @Override public AgentSession openSession(AgentSessionInit init) { throw new UnsupportedOperationException(); }
        };
    }

    private static final String WAIT_JSON = "{\"action\":{\"type\":\"WAIT\"}}";

    @Test
    void concurrentInvocationsAllSucceed() throws Exception {
        var callCount = new AtomicInteger(0);
        AgentProvider slowProvider = new AgentProvider() {
            @Override
            public Multi<AgentEvent> invoke(io.casehub.platform.agent.AgentSessionConfig config) {
                callCount.incrementAndGet();
                try {Thread.sleep(50);} catch (InterruptedException e) {Thread.currentThread().interrupt();}
                return Multi.createFrom().item((AgentEvent) new AgentEvent.TextDelta(WAIT_JSON));
            }

            @Override
            public AgentSession openSession(AgentSessionInit init) {throw new UnsupportedOperationException();}
        };

        var service = new AgentInvocationService(slowProvider, 0, 60, 2, 100);
        int agents  = 6;
        var barrier = new CyclicBarrier(agents);
        var threads = new java.util.ArrayList<Thread>();

        for (int i = 0; i < agents; i++) {
            int id = i;
            threads.add(Thread.ofVirtual().start(() -> {
                try {
                    barrier.await();
                    service.invoke("system", "user", "agent-" + id);
                } catch (Exception e) { /* ignore */ }
            }));
        }
        for (var t : threads) {t.join();}

        assertThat(service.metrics().totalCalls()).isEqualTo(agents);
    }

    @Test
    void retryWithJitterOnFailureThenSuccess() {
        var callCount = new AtomicInteger(0);
        AgentProvider failThenSucceed = new AgentProvider() {
            @Override public Multi<AgentEvent> invoke(io.casehub.platform.agent.AgentSessionConfig config) {
                if (callCount.getAndIncrement() == 0) {
                    throw new RuntimeException("transient failure");
                }
                return Multi.createFrom().item((AgentEvent) new AgentEvent.TextDelta(WAIT_JSON));
            }
            @Override public AgentSession openSession(AgentSessionInit init) { throw new UnsupportedOperationException(); }
        };

        var service = new AgentInvocationService(failThenSucceed, 5, 60, 2, 50);
        AgentResponse response = service.invoke("system", "user", "test-agent");

        assertThat(response).isNotNull();
        assertThat(callCount.get()).isEqualTo(2);
        assertThat(service.metrics().retries()).isEqualTo(1);
    }

    @Test
    void fallsBackToIdleAfterMaxRetries() {
        AgentProvider alwaysFails = new AgentProvider() {
            @Override public Multi<AgentEvent> invoke(io.casehub.platform.agent.AgentSessionConfig config) {
                throw new RuntimeException("permanent failure");
            }
            @Override public AgentSession openSession(AgentSessionInit init) { throw new UnsupportedOperationException(); }
        };

        var service = new AgentInvocationService(alwaysFails, 5, 60, 2, 50);
        AgentResponse response = service.invoke("system", "user", "test-agent");

        assertThat(response).isNotNull();
        assertThat(response.action().type()).isEqualTo(io.casehub.examples.manor.model.ActionType.WAIT);
        assertThat(service.metrics().fallbacks()).isEqualTo(1);
    }

    @Test
    void parsesValidJsonResponse() {
        String json = "{\"thinking\":\"hmm\",\"dialogue\":\"Hello!\",\"action\":{\"type\":\"MOVE\",\"target\":\"kitchen\"}}";
        var service = new AgentInvocationService(stubProvider(json), 5, 60, 2, 100);

        AgentResponse response = service.invoke("system", "user", "test-agent");

        assertThat(response.thinking()).isEqualTo("hmm");
        assertThat(response.dialogue()).isEqualTo("Hello!");
        assertThat(response.action().type()).isEqualTo(io.casehub.examples.manor.model.ActionType.MOVE);
        assertThat(response.action().target()).isEqualTo("kitchen");
    }

    @Test
    void metricsTrackLatency() {
        var service = new AgentInvocationService(stubProvider(WAIT_JSON), 5, 60, 2, 100);
        service.invoke("system", "user", "agent-1");
        service.invoke("system", "user", "agent-2");

        var metrics = service.metrics();
        assertThat(metrics.totalCalls()).isEqualTo(2);
        assertThat(metrics.averageLatencyMs()).isGreaterThanOrEqualTo(0);
    }
}
