package io.casehub.examples.manor.agent;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.casehub.platform.agent.AgentSessionLimitException;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatedAgentProviderTest {

    @Test
    void callWithinLimitCompletesAndReleasesPermit() {
        var gated = new GatedAgentProvider(stubProvider("hello"), 2, Duration.ofSeconds(5));

        String result = collectText(gated.invoke(config()));

        assertThat(result).isEqualTo("hello");
        assertThat(gated.availablePermits()).isEqualTo(2);
    }

    @Test
    void permitReleasedOnDelegateFailure() {
        AgentProvider failing = new StubProvider() {
            @Override
            public Multi<AgentEvent> invoke(AgentSessionConfig config) {
                return Multi.createFrom().failure(new RuntimeException("boom"));
            }
        };
        var gated = new GatedAgentProvider(failing, 1, Duration.ofSeconds(5));

        assertThatThrownBy(() -> collectText(gated.invoke(config())))
                .hasMessageContaining("boom");
        assertThat(gated.availablePermits()).isEqualTo(1);
    }

    @Test
    void permitReleasedWhenDelegateInvokeThrowsSynchronously() {
        AgentProvider exploding = new StubProvider() {
            @Override
            public Multi<AgentEvent> invoke(AgentSessionConfig config) {
                throw new IllegalStateException("sync explosion");
            }
        };
        var gated = new GatedAgentProvider(exploding, 1, Duration.ofSeconds(5));

        assertThatThrownBy(() -> collectText(gated.invoke(config())))
                .hasMessageContaining("sync explosion");
        assertThat(gated.availablePermits()).isEqualTo(1);
    }

    @Test
    void excessCallQueuesUntilPermitFreed() throws Exception {
        var firstStarted = new CountDownLatch(1);
        var holdFirst = new CountDownLatch(1);
        var callOrder = new AtomicInteger();
        AgentProvider delayed = new StubProvider() {
            @Override
            public Multi<AgentEvent> invoke(AgentSessionConfig config) {
                int n = callOrder.incrementAndGet();
                if (n == 1) {
                    return Multi.createFrom().emitter(em -> {
                        firstStarted.countDown();
                        try { holdFirst.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        em.emit(new AgentEvent.TextDelta("first"));
                        em.complete();
                    });
                }
                return Multi.createFrom().item(new AgentEvent.TextDelta("second"));
            }
        };
        var gated = new GatedAgentProvider(delayed, 1, Duration.ofSeconds(10));
        var results = new ConcurrentLinkedQueue<String>();
        var allDone = new CountDownLatch(2);

        Thread.ofVirtual().start(() -> {
            results.add(collectText(gated.invoke(config())));
            allDone.countDown();
        });
        assertThat(firstStarted.await(5, TimeUnit.SECONDS)).isTrue();

        Thread.ofVirtual().start(() -> {
            results.add(collectText(gated.invoke(config())));
            allDone.countDown();
        });
        Thread.sleep(100);
        assertThat(gated.availablePermits()).isEqualTo(0);

        holdFirst.countDown();
        assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(results).containsExactlyInAnyOrder("first", "second");
        assertThat(gated.availablePermits()).isEqualTo(1);
    }

    @Test
    void acquireTimeoutReturnsSessionLimitFailure() throws Exception {
        var holdForever = new CountDownLatch(1);
        AgentProvider slow = new StubProvider() {
            @Override
            public Multi<AgentEvent> invoke(AgentSessionConfig config) {
                return Multi.createFrom().emitter(em -> {
                    try { holdForever.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    em.emit(new AgentEvent.TextDelta("done"));
                    em.complete();
                });
            }
        };
        var gated = new GatedAgentProvider(slow, 1, Duration.ofMillis(200));

        Thread.ofVirtual().start(() -> collectText(gated.invoke(config())));
        Thread.sleep(100);

        assertThatThrownBy(() -> collectText(gated.invoke(config())))
                .isInstanceOf(AgentSessionLimitException.class);

        holdForever.countDown();
    }

    @Test
    void openSessionDelegatesToUnderlyingProvider() {
        var gated = new GatedAgentProvider(stubProvider("x"), 2, Duration.ofSeconds(5));

        assertThatThrownBy(() -> gated.openSession(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- helpers ---

    private static AgentSessionConfig config() {
        return AgentSessionConfig.of("system", "user");
    }

    private static String collectText(Multi<AgentEvent> multi) {
        return multi
                .filter(e -> e instanceof AgentEvent.TextDelta)
                .map(e -> ((AgentEvent.TextDelta) e).text())
                .collect().with(Collectors.joining())
                .await().atMost(Duration.ofSeconds(30));
    }

    private static AgentProvider stubProvider(String text) {
        return new StubProvider() {
            @Override
            public Multi<AgentEvent> invoke(AgentSessionConfig config) {
                return Multi.createFrom().item(new AgentEvent.TextDelta(text));
            }
        };
    }

    private static abstract class StubProvider implements AgentProvider {
        @Override
        public AgentSession openSession(AgentSessionInit init) {
            throw new UnsupportedOperationException();
        }
    }
}
