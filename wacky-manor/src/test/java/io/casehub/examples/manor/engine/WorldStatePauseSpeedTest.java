package io.casehub.examples.manor.engine;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class WorldStatePauseSpeedTest {

    private WorldState world() {
        return new WorldState(Map.of(), Map.of());
    }

    @Test
    void defaults_to_not_paused() {
        assertThat(world().isPaused()).isFalse();
    }

    @Test
    void defaults_to_speed_1x() {
        assertThat(world().speedMultiplier()).isEqualTo(1.0);
    }

    @Test
    void pause_and_resume() {
        var w = world();
        w.setPaused(true);
        assertThat(w.isPaused()).isTrue();
        w.setPaused(false);
        assertThat(w.isPaused()).isFalse();
    }

    @Test
    void speed_multiplier_set_and_get() {
        var w = world();
        w.setSpeedMultiplier(2.0);
        assertThat(w.speedMultiplier()).isEqualTo(2.0);
    }

    @Test
    void speed_clamped_low() {
        var w = world();
        w.setSpeedMultiplier(0.1);
        assertThat(w.speedMultiplier()).isEqualTo(0.25);
    }

    @Test
    void speed_clamped_high() {
        var w = world();
        w.setSpeedMultiplier(20.0);
        assertThat(w.speedMultiplier()).isEqualTo(8.0);
    }
}
