package io.casehub.examples.manor.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.casehub.examples.manor.model.CompletionReason;
import io.casehub.examples.manor.model.ProfileMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TranscriptRecorder {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final List<Event> events = new ArrayList<>();
    private final String modelIdentifier;
    private final String gitCommitHash;

    public TranscriptRecorder(String modelIdentifier, String gitCommitHash) {
        this.modelIdentifier = modelIdentifier;
        this.gitCommitHash = gitCommitHash;
    }

    public void record(Event event) {
        events.add(event);
    }

    public RunResult toRunResult(ProfileMode profile, int runNumber,
                                  CompletionReason verdict, int totalTurns,
                                  long durationMs) {
        return new RunResult(profile, runNumber, verdict, totalTurns,
                durationMs, List.copyOf(events), modelIdentifier,
                Instant.now(), gitCommitHash);
    }

    public static void writeJson(RunResult result, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        MAPPER.writeValue(file.toFile(), result);
    }

    public static RunResult readJson(Path file) throws IOException {
        return MAPPER.readValue(file.toFile(), RunResult.class);
    }

    public record Event(int turn, String characterId, String type,
                         String action, String target, String dialogue,
                         String thinking, String aside, String result) {}

    public record RunResult(ProfileMode profile, int runNumber,
                             CompletionReason verdict, int totalTurns,
                             long durationMs, List<Event> events,
                             String modelIdentifier, Instant timestamp,
                             String gitCommitHash) {}
}
