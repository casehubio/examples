package io.casehub.examples.manor.experiment;

import io.casehub.examples.manor.model.CompletionReason;
import io.casehub.examples.manor.model.ProfileMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptRecorderTest {

    @Test
    void records_events_and_builds_run_result() {
        var recorder = new TranscriptRecorder("claude-sonnet-4", "abc123");
        recorder.record(new TranscriptRecorder.Event(
                1, "hooded-claw", "action", "MOVE", "kitchen",
                null, "I should find the poison", null, "You moved to Kitchen."));
        recorder.record(new TranscriptRecorder.Event(
                1, "hooded-claw", "dialogue", null, null,
                "Oh what a lovely kitchen!", null, null, null));

        var result = recorder.toRunResult(
                ProfileMode.BASELINE, 1, CompletionReason.DAWN, 42, 5000L);

        assertThat(result.profile()).isEqualTo(ProfileMode.BASELINE);
        assertThat(result.runNumber()).isEqualTo(1);
        assertThat(result.verdict()).isEqualTo(CompletionReason.DAWN);
        assertThat(result.totalTurns()).isEqualTo(42);
        assertThat(result.events()).hasSize(2);
        assertThat(result.modelIdentifier()).isEqualTo("claude-sonnet-4");
        assertThat(result.gitCommitHash()).isEqualTo("abc123");
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    void json_round_trip(@TempDir Path tempDir) throws Exception {
        var recorder = new TranscriptRecorder("claude-sonnet-4", "abc123");
        recorder.record(new TranscriptRecorder.Event(
                1, "penelope-pitstop", "dialogue", null, null,
                "Why, darlin'!", null, null, null));

        var result = recorder.toRunResult(
                ProfileMode.JUNGIAN, 2, CompletionReason.DAWN, 180, 12000L);

        var file = tempDir.resolve("test-run.json");
        TranscriptRecorder.writeJson(result, file);

        assertThat(file).exists();

        var loaded = TranscriptRecorder.readJson(file);
        assertThat(loaded.profile()).isEqualTo(ProfileMode.JUNGIAN);
        assertThat(loaded.runNumber()).isEqualTo(2);
        assertThat(loaded.verdict()).isEqualTo(CompletionReason.DAWN);
        assertThat(loaded.events()).hasSize(1);
        assertThat(loaded.events().get(0).dialogue()).isEqualTo("Why, darlin'!");
    }
}
