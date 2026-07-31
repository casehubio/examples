package io.casehub.examples.manor.experiment;

import io.casehub.examples.manor.model.CompletionReason;
import io.casehub.examples.manor.model.ProfileMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultsSummaryTest {

    @Test
    void generates_comparison_report(@TempDir Path tempDir) throws Exception {
        writeFixture(tempDir, ProfileMode.BASELINE, 1,
                CompletionReason.POISONED, 91, 45000L);
        writeFixture(tempDir, ProfileMode.BASELINE, 2,
                CompletionReason.TURN_LIMIT, 180, 112000L);
        writeFixture(tempDir, ProfileMode.JUNGIAN, 1,
                CompletionReason.POISONED, 67, 38000L);

        var outputFile = tempDir.resolve("COMPARISON.md");
        ResultsSummary.generate(tempDir, outputFile);

        assertThat(outputFile).exists();
        String content = Files.readString(outputFile);
        assertThat(content).contains("## Verdict Breakdown");
        assertThat(content).contains("BASELINE");
        assertThat(content).contains("JUNGIAN");
        assertThat(content).contains("POISONED");
        assertThat(content).contains("TURN_LIMIT");
    }

    @Test
    void verdict_gates_show_comparison(@TempDir Path tempDir) throws Exception {
        writeFixture(tempDir, ProfileMode.BASELINE, 1,
                CompletionReason.POISONED, 91, 45000L);
        writeFixture(tempDir, ProfileMode.BASELINE, 2,
                CompletionReason.POISONED, 100, 50000L);
        writeFixture(tempDir, ProfileMode.JUNGIAN, 1,
                CompletionReason.POISONED, 67, 38000L);
        writeFixture(tempDir, ProfileMode.JUNGIAN, 2,
                CompletionReason.POISONED, 55, 30000L);

        var outputFile = tempDir.resolve("COMPARISON.md");
        ResultsSummary.generate(tempDir, outputFile);

        String content = Files.readString(outputFile);
        assertThat(content).contains("## Verdict Gates");
        assertThat(content).contains("Layer 1 vs 0");
        assertThat(content).contains("Avg turns:");
    }

    @Test
    void handles_missing_layers(@TempDir Path tempDir) throws Exception {
        writeFixture(tempDir, ProfileMode.BASELINE, 1,
                CompletionReason.POISONED, 91, 45000L);

        var outputFile = tempDir.resolve("COMPARISON.md");
        ResultsSummary.generate(tempDir, outputFile);

        String content = Files.readString(outputFile);
        assertThat(content).contains("Insufficient data");
    }

    private void writeFixture(Path dir, ProfileMode profile, int run,
                               CompletionReason verdict, int turns, long duration)
            throws Exception {
        var result = new TranscriptRecorder.RunResult(
                profile, run, verdict, turns, duration,
                List.of(), "test-model", Instant.now(), "abc123");
        var file = dir.resolve(
                profile.name().toLowerCase() + "-run-" + run + ".json");
        TranscriptRecorder.writeJson(result, file);
    }
}
