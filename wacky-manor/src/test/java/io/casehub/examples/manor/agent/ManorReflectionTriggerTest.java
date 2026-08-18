package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManorReflectionTriggerTest {

    @Test
    void firesAfterMaxUnreflectedCount() {
        var trigger = new ManorReflectionTrigger(3, 100.0);

        assertThat(trigger.shouldReflect("a1", 0.5)).isFalse();
        assertThat(trigger.shouldReflect("a1", 0.5)).isFalse();
        assertThat(trigger.shouldReflect("a1", 0.5)).isTrue();
    }

    @Test
    void firesOnImportanceThreshold() {
        var trigger = new ManorReflectionTrigger(100, 2.0);

        assertThat(trigger.shouldReflect("a1", 0.9)).isFalse();
        assertThat(trigger.shouldReflect("a1", 0.9)).isFalse();
        assertThat(trigger.shouldReflect("a1", 0.9)).isTrue();
    }

    @Test
    void resetClearsBothCounters() {
        var trigger = new ManorReflectionTrigger(3, 100.0);

        trigger.shouldReflect("a1", 0.5);
        trigger.shouldReflect("a1", 0.5);
        trigger.reset("a1");

        assertThat(trigger.shouldReflect("a1", 0.5)).isFalse();
        assertThat(trigger.shouldReflect("a1", 0.5)).isFalse();
        assertThat(trigger.shouldReflect("a1", 0.5)).isTrue();
    }

    @Test
    void tracksAgentsIndependently() {
        var trigger = new ManorReflectionTrigger(2, 100.0);

        trigger.shouldReflect("a1", 0.5);
        trigger.shouldReflect("a2", 0.5);

        assertThat(trigger.shouldReflect("a1", 0.5)).isTrue();
        assertThat(trigger.shouldReflect("a2", 0.5)).isTrue();
    }
}
