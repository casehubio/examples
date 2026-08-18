package io.casehub.examples.manor.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterStateTest {

    @Test
    void inventory_concurrent_add_remove_does_not_throw() throws Exception {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of("item-a"));
        var latch = new CountDownLatch(1);
        var threads = new ArrayList<Thread>();
        for (int i = 0; i < 10; i++) {
            int idx = i;
            threads.add(Thread.ofVirtual().start(() -> {
                try { latch.await(); } catch (InterruptedException e) { return; }
                state.addItem("item-" + idx);
                state.hasItem("item-" + idx);
                state.removeItem("item-" + idx);
            }));
        }
        latch.countDown();
        for (var t : threads) t.join(Duration.ofSeconds(5));
    }

    @Test
    void capability_tags_empty_by_default() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        assertThat(state.capabilityTags()).isEmpty();
    }

    @Test
    void capability_tags_set_and_retrieved() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        state.setCapabilityTags(java.util.Set.of("perception", "villain"));
        assertThat(state.capabilityTags()).containsExactlyInAnyOrder("perception", "villain");
    }

    @Test
    void capability_tags_are_immutable_copy() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        state.setCapabilityTags(java.util.Set.of("perception"));
        var tags = state.capabilityTags();
        assertThat(tags).isUnmodifiable();
    }

    @Test
    void plans_empty_by_default() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        assertThat(state.plans()).isEmpty();
    }

    @Test
    void setPlan_and_retrieve() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        var step  = new PlanStep("s1", "Find the poison", PlanStepStatus.PENDING);
        var plan  = new AgentPlan("protect-penelope", List.of(step), "need to protect", 1, 1, 0);
        state.setPlan("protect-penelope", plan);
        assertThat(state.plans()).containsKey("protect-penelope");
        assertThat(state.plans().get("protect-penelope").steps()).hasSize(1);
    }

    @Test
    void removePlan_removes_by_goalName() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        var plan  = new AgentPlan("goal-a", List.of(), "r", 1, 1, 0);
        state.setPlan("goal-a", plan);
        state.removePlan("goal-a");
        assertThat(state.plans()).isEmpty();
    }

    @Test
    void currentThinking_null_by_default() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        assertThat(state.currentThinking()).isNull();
    }

    @Test
    void currentThinking_set_and_retrieved() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        state.setCurrentThinking("I see the poison on the shelf");
        assertThat(state.currentThinking()).isEqualTo("I see the poison on the shelf");
    }

}
