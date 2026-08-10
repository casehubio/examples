package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.model.TickSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AssertionRegistryTest {

    @Test
    void assertion_not_satisfied_initially() {
        var registry = new AssertionRegistry();
        registry.register("test", snap -> false);
        assertThat(registry.wasSatisfied("test")).isFalse();
    }

    @Test
    void assertion_satisfied_after_matching_tick() {
        var registry = new AssertionRegistry();
        registry.register("test", snap -> snap.tick() == 3);
        registry.evaluate(new TickSnapshot(1, Map.of(), List.of(), null));
        registry.evaluate(new TickSnapshot(2, Map.of(), List.of(), null));
        registry.evaluate(new TickSnapshot(3, Map.of(), List.of(), null));
        assertThat(registry.wasSatisfied("test")).isTrue();
        assertThat(registry.firstSatisfiedTick("test")).isEqualTo(3);
    }

    @Test
    void unknown_assertion_returns_false() {
        var registry = new AssertionRegistry();
        assertThat(registry.wasSatisfied("nonexistent")).isFalse();
    }

    @Test
    void first_satisfied_tick_returns_negative_when_never_satisfied() {
        var registry = new AssertionRegistry();
        registry.register("test", snap -> false);
        registry.evaluate(new TickSnapshot(1, Map.of(), List.of(), null));
        assertThat(registry.firstSatisfiedTick("test")).isEqualTo(-1);
    }
}
