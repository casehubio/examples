package io.casehub.examples.manor.web;

import io.casehub.examples.manor.engine.WorldState;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManorResourceControlTest {

    private ManorResource resource() {
        var r = new ManorResource();
        r.eventBus = new ManorEventBus();
        return r;
    }

    @Test
    void pause_returns_404_when_no_scenario() {
        var resp = resource().pauseScenario();
        assertThat(resp.getStatus()).isEqualTo(404);
    }

    @Test
    void resume_returns_404_when_no_scenario() {
        var resp = resource().resumeScenario();
        assertThat(resp.getStatus()).isEqualTo(404);
    }

    @Test
    void speed_returns_404_when_no_scenario() {
        var resp = resource().setSpeed(2.0);
        assertThat(resp.getStatus()).isEqualTo(404);
    }

    @Test
    void pause_returns_ok_with_active_scenario() {
        var r = resource();
        r.activeWorld = new WorldState(Map.of(), Map.of());
        r.eventBus.setActiveWorld(r.activeWorld);
        var resp = r.pauseScenario();
        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(r.activeWorld.isPaused()).isTrue();
    }

    @Test
    void resume_unpauses_active_scenario() {
        var r = resource();
        r.activeWorld = new WorldState(Map.of(), Map.of());
        r.activeWorld.setPaused(true);
        r.eventBus.setActiveWorld(r.activeWorld);
        var resp = r.resumeScenario();
        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(r.activeWorld.isPaused()).isFalse();
    }

    @Test
    void speed_sets_multiplier_on_active_scenario() {
        var r = resource();
        r.activeWorld = new WorldState(Map.of(), Map.of());
        r.eventBus.setActiveWorld(r.activeWorld);
        var resp = r.setSpeed(4.0);
        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(r.activeWorld.speedMultiplier()).isEqualTo(4.0);
    }
}
