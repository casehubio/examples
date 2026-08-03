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
}
