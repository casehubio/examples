package io.casehub.examples.manor.experiment;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CognitiveDeltaCaptureTest {

    @Test void noDeltaWhenStateUnchanged() {
        var capture = new CognitiveDeltaCapture();
        var state = Map.of("Mood", "neutral", "Goals", "explore");
        capture.record(1, "agent-1", state);
        capture.record(2, "agent-1", state);
        assertThat(capture.deltas().stream().filter(d -> d.tick() == 2)).isEmpty();
    }

    @Test void deltaEmittedWhenSectionChanges() {
        var capture = new CognitiveDeltaCapture();
        capture.record(1, "agent-1", Map.of("Mood", "neutral"));
        capture.record(2, "agent-1", Map.of("Mood", "anxious"));
        var tick2 = capture.deltas().stream().filter(d -> d.tick() == 2).toList();
        assertThat(tick2).hasSize(1);
        assertThat(tick2.getFirst().changedSections()).containsKey("Mood");
    }

    @Test void deltaEmittedWhenSectionAdded() {
        var capture = new CognitiveDeltaCapture();
        capture.record(1, "agent-1", Map.of("Mood", "neutral"));
        capture.record(2, "agent-1", Map.of("Mood", "neutral", "Narrative", "emerging arc"));
        var tick2 = capture.deltas().stream().filter(d -> d.tick() == 2).toList();
        assertThat(tick2).hasSize(1);
        assertThat(tick2.getFirst().addedSections()).containsKey("Narrative");
    }

    @Test void deltaEmittedWhenSectionRemoved() {
        var capture = new CognitiveDeltaCapture();
        capture.record(1, "agent-1", Map.of("Mood", "neutral", "Goals", "explore"));
        capture.record(2, "agent-1", Map.of("Mood", "neutral"));
        var tick2 = capture.deltas().stream().filter(d -> d.tick() == 2).toList();
        assertThat(tick2).hasSize(1);
        assertThat(tick2.getFirst().removedSections()).contains("Goals");
    }
}
