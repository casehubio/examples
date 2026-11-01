package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void appendTick_createsZipArchive() throws Exception {
        var store = new TranscriptStore(tempDir, "GENERIC");

        store.appendTick(1,
                         Map.of("penelope", "What do you see?"),
                         Map.of("penelope", "{\"dialogue\":\"I see a door.\"}"));

        Thread.sleep(1000);

        var corpusDir = tempDir.resolve("generic");
        assertThat(corpusDir).isDirectory();
        try (var files = Files.list(corpusDir)) {
            var zips = files.filter(p -> p.toString().endsWith(".zip")).toList();
            assertThat(zips).isNotEmpty();
        }
    }

    @Test
    void appendTick_skipsAgentsWithNoResponse() throws Exception {
        var store = new TranscriptStore(tempDir, "BASELINE");

        store.appendTick(5,
                         Map.of("penelope", "prompt text"),
                         Map.of());

        Thread.sleep(500);

        var corpusDir = tempDir.resolve("baseline");
        assertThat(corpusDir).isDirectory();
        try (var files = Files.list(corpusDir)) {
            var zips = files.filter(p -> p.toString().endsWith(".zip")).toList();
            assertThat(zips).isEmpty();
        }
    }

    @Test
    void toJson_producesValidStructure() {
        var json = TranscriptStore.toJson(7, "penelope", "2026-11-01T00:00:00Z",
                                          "What do you see?", "{\"dialogue\":\"I see a door.\"}");
        assertThat(json).startsWith("{");
        assertThat(json).endsWith("}");
        assertThat(json).contains("\"tick\":7");
        assertThat(json).contains("\"agentId\":\"penelope\"");
        assertThat(json).contains("\"timestamp\":\"2026-11-01T00:00:00Z\"");
        assertThat(json).contains("What do you see?");
        assertThat(json).contains("I see a door.");
    }

    @Test
    void toJson_escapesSpecialCharacters() {
        var json = TranscriptStore.toJson(1, "agent-1", "2026-11-01T00:00:00Z",
                                          "line1\nline2\ttab", "quote\"here\\back");
        assertThat(json).contains("\\n");
        assertThat(json).contains("\\t");
        assertThat(json).contains("\\\"");
        assertThat(json).contains("\\\\");
        assertThat(json).doesNotContain("\n");
    }

    @Test
    void toJson_handlesMultipleTicksWithPadding() {
        var json = TranscriptStore.toJson(42, "muttley", "2026-11-01T00:00:00Z", "p", "r");
        assertThat(json).contains("\"tick\":42");
    }
}
