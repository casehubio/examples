package io.casehub.examples.manor.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorldStateConcurrencyTest {

    private WorldState world;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
    }

    @Test
    void concurrentAddEventDoesNotCorrupt() throws Exception {
        int threads = 10;
        int eventsPerThread = 100;
        var barrier = new CyclicBarrier(threads);
        var errors = new AtomicInteger(0);

        var threadList = new ArrayList<Thread>();
        for (int t = 0; t < threads; t++) {
            int threadId = t;
            threadList.add(Thread.ofVirtual().start(() -> {
                try {
                    barrier.await();
                    for (int i = 0; i < eventsPerThread; i++) {
                        world.addEvent("action", "agent-" + threadId, "entrance-hall",
                            "Event " + threadId + "-" + i);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }));
        }
        for (var t : threadList) t.join();

        assertThat(errors.get()).isZero();
        assertThat(world.allEvents()).hasSize(threads * eventsPerThread);
    }

    @Test
    void concurrentTryTakeObjectIsAtomic() throws Exception {
        int threads = 10;
        var barrier = new CyclicBarrier(threads);
        var takenCount = new AtomicInteger(0);

        var threadList = new ArrayList<Thread>();
        for (int t = 0; t < threads; t++) {
            threadList.add(Thread.ofVirtual().start(() -> {
                try {
                    barrier.await();
                    if (world.tryTakeObject("poison")) {
                        takenCount.incrementAndGet();
                    }
                } catch (Exception e) { /* barrier interrupt */ }
            }));
        }
        for (var t : threadList) t.join();

        assertThat(takenCount.get()).isEqualTo(1);
    }

    @Test
    void charactersReturnsUnmodifiableView() {
        var snapshot = world.characters();
        assertThatThrownBy(() -> snapshot.put("intruder", null))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void concurrentEventLogAndRecentEventsDoNotInterfere() throws Exception {
        int threads = 5;
        int eventsPerThread = 50;
        var barrier = new CyclicBarrier(threads + 1);
        var errors = new AtomicInteger(0);

        var threadList = new ArrayList<Thread>();
        for (int t = 0; t < threads; t++) {
            int threadId = t;
            threadList.add(Thread.ofVirtual().start(() -> {
                try {
                    barrier.await();
                    for (int i = 0; i < eventsPerThread; i++) {
                        world.addEvent("dialogue", "agent-" + threadId, "entrance-hall",
                            "Speech " + threadId + "-" + i);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }));
        }
        threadList.add(Thread.ofVirtual().start(() -> {
            try {
                barrier.await();
                for (int i = 0; i < eventsPerThread; i++) {
                    world.recentEvents("entrance-hall", 5);
                }
            } catch (Exception e) {
                errors.incrementAndGet();
            }
        }));

        for (var t : threadList) t.join();
        assertThat(errors.get()).isZero();
    }
}
