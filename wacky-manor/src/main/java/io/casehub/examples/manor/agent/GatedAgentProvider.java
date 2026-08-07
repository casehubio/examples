package io.casehub.examples.manor.agent;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.casehub.platform.agent.AgentSessionLimitException;
import io.smallrye.mutiny.Multi;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Application-level concurrency gate for {@link AgentProvider}.
 *
 * <p>Wraps a delegate provider with a blocking-with-timeout semaphore so that excess
 * calls queue rather than hitting the platform's fail-fast semaphore. Size this gate
 * at or below the platform's {@code max-concurrent-sessions} to prevent rejections.
 */
public final class GatedAgentProvider implements AgentProvider {

    private final AgentProvider delegate;
    private final Semaphore gate;
    private final int maxConcurrent;
    private final Duration acquireTimeout;

    public GatedAgentProvider(AgentProvider delegate, int maxConcurrent, Duration acquireTimeout) {
        this.delegate = delegate;
        this.maxConcurrent = maxConcurrent;
        this.gate = new Semaphore(maxConcurrent, true);
        this.acquireTimeout = acquireTimeout;
    }

    @Override
    public Multi<AgentEvent> invoke(AgentSessionConfig config) {
        try {
            if (!gate.tryAcquire(acquireTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                return Multi.createFrom().failure(new AgentSessionLimitException(maxConcurrent));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Multi.createFrom().failure(e);
        }
        try {
            return delegate.invoke(config)
                    .onTermination().invoke(() -> gate.release());
        } catch (Exception e) {
            gate.release();
            return Multi.createFrom().failure(e);
        }
    }

    @Override
    public AgentSession openSession(AgentSessionInit init) {
        return delegate.openSession(init);
    }

    int availablePermits() {
        return gate.availablePermits();
    }
}
