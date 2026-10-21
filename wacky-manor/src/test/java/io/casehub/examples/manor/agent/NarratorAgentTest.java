package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.model.ManorEvent;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentSession;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class NarratorAgentTest {

    static final AgentSession echoSession = new AgentSession() {
        @Override public Multi<AgentEvent> query(String prompt) {
            return Multi.createFrom().item(new AgentEvent.TextDelta("DRAMATIC narration!"));
        }
        @Override public Uni<Void> interrupt() { return Uni.createFrom().voidItem(); }
        @Override public void clear() {}
        @Override public void close(Duration maxWait) {}
    };

    @Test
    void collect_5_events_then_tick_dispatches_narration() {
        var dispatched = new ArrayList<String>();
        var agent = new NarratorAgent(
                new MechanicalCompactor(), echoSession,
                null, null, 5, 15);
        agent.testSubscribe(dispatched::add);

        for (int i = 0; i < 5; i++) {
            agent.collect(new ManorEvent.Action(Instant.now(), "char-" + i,
                    "kitchen", "event " + i, null, null, null, null));
        }
        agent.tickNow();

        assertThat(dispatched).hasSize(1);
        assertThat(dispatched.get(0)).isEqualTo("DRAMATIC narration!");
    }

    @Test
    void below_threshold_does_not_narrate() {
        var dispatched = new ArrayList<String>();
        var agent = new NarratorAgent(
                new MechanicalCompactor(), echoSession,
                null, null, 5, 15);
        agent.testSubscribe(dispatched::add);

        for (int i = 0; i < 3; i++) {
            agent.collect(new ManorEvent.Action(Instant.now(), "char-" + i,
                    "kitchen", "event " + i, null, null, null, null));
        }
        agent.tickAt(System.currentTimeMillis());

        assertThat(dispatched).isEmpty();
    }

    @Test
    void timer_fires_when_events_below_threshold_but_old_enough() {
        var dispatched = new ArrayList<String>();
        var agent = new NarratorAgent(
                new MechanicalCompactor(), echoSession,
                null, null, 5, 15);
        agent.testSubscribe(dispatched::add);

        agent.collect(new ManorEvent.Action(Instant.now(), "char-0",
                "kitchen", "event 0", null, null, null, null));
        agent.tickAt(System.currentTimeMillis() + 16_000);

        assertThat(dispatched).hasSize(1);
    }

    @Test
    void collect_after_stop_does_not_throw() {
        var agent = new NarratorAgent(
                new MechanicalCompactor(), echoSession,
                null, null, 5, 15);
        agent.stop();
        agent.collect(new ManorEvent.Action(Instant.now(), "x", "kitchen", "event", null, null, null, null));
    }

    @Test
    void flush_captures_remaining_events_below_threshold() {
        var dispatched = new ArrayList<String>();
        var agent = new NarratorAgent(
                new MechanicalCompactor(), echoSession,
                null, null, 5, 15);
        agent.testSubscribe(dispatched::add);

        for (int i = 0; i < 3; i++) {
            agent.collect(new ManorEvent.Action(Instant.now(), "char-" + i,
                    "kitchen", "event " + i, null, null, null, null));
        }
        agent.flushNow();

        assertThat(dispatched).hasSize(1);
        assertThat(dispatched.get(0)).isEqualTo("DRAMATIC narration!");
    }
}
