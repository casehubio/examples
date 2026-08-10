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
    void currentPlan_null_by_default() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        assertThat(state.currentPlan()).isNull();
    }

    @Test
    void currentPlan_set_and_retrieved() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        state.setCurrentPlan("Step 1: get the poison");
        assertThat(state.currentPlan()).isEqualTo("Step 1: get the poison");
    }

    @Test
    void dynamicGoals_empty_by_default() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        assertThat(state.dynamicGoals()).isEmpty();
    }

    @Test
    void addDynamicGoal_and_retrieve() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        state.addDynamicGoal(new io.casehub.examples.manor.model.DynamicGoal("protect-tea", "Stop the poison", 1));
        assertThat(state.dynamicGoals()).hasSize(1);
        assertThat(state.dynamicGoals().get(0).name()).isEqualTo("protect-tea");
    }

    @Test
    void dropDynamicGoal_removes_by_normalized_name() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        state.addDynamicGoal(new io.casehub.examples.manor.model.DynamicGoal("protect-tea", "Stop the poison", 1));
        state.dropDynamicGoal("Protect-Tea");
        assertThat(state.dynamicGoals()).isEmpty();
    }

    @Test
    void dropAllDynamicGoals_clears_all() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        state.addDynamicGoal(new io.casehub.examples.manor.model.DynamicGoal("goal-a", "A", 1));
        state.addDynamicGoal(new io.casehub.examples.manor.model.DynamicGoal("goal-b", "B", 2));
        state.dropAllDynamicGoals();
        assertThat(state.dynamicGoals()).isEmpty();
    }

    @Test
    void addDynamicGoal_replaces_existing_with_same_name() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        state.addDynamicGoal(new io.casehub.examples.manor.model.DynamicGoal("protect-tea", "V1", 1));
        state.addDynamicGoal(new io.casehub.examples.manor.model.DynamicGoal("Protect-Tea", "V2", 2));
        assertThat(state.dynamicGoals()).hasSize(1);
        assertThat(state.dynamicGoals().get(0).description()).isEqualTo("V2");
    }

    @Test
    void dynamicGoals_capped_evicts_oldest() {
        var state = new CharacterState("test", "Test", "room", 0.5, List.of());
        for (int i = 0; i < 6; i++) {
            state.addDynamicGoal(new io.casehub.examples.manor.model.DynamicGoal("goal-" + i, "G" + i, i));
        }
        assertThat(state.dynamicGoals()).hasSize(5);
        assertThat(state.dynamicGoals().stream().map(io.casehub.examples.manor.model.DynamicGoal::name).toList()).doesNotContain("goal-0");
    }
}
