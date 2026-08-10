package io.casehub.examples.manor.experiment;

import io.casehub.examples.manor.model.ProfileMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ResultsSummary {

    public static void generate(Path inputDir, Path outputFile) throws IOException {
        var results = loadResults(inputDir);
        var grouped = results.stream().collect(
                Collectors.groupingBy(TranscriptRecorder.RunResult::profile,
                        LinkedHashMap::new, Collectors.toList()));

        var sb = new StringBuilder();
        sb.append("# Staged Layer Comparison — Results\n\n");
        sb.append("Generated from ").append(results.size()).append(" run(s)\n\n");

        appendVerdictBreakdown(sb, grouped);
        appendVerdictGates(sb, grouped);

        Files.writeString(outputFile, sb.toString());
    }

    private static List<TranscriptRecorder.RunResult> loadResults(Path dir) throws IOException {
        var results = new ArrayList<TranscriptRecorder.RunResult>();
        try (var files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.getFileName().toString().matches(".*-run-\\d+\\.json")).toList()) {
                results.add(TranscriptRecorder.readJson(file));
            }
        }
        results.sort(Comparator.comparing(TranscriptRecorder.RunResult::profile)
                .thenComparingInt(TranscriptRecorder.RunResult::runNumber));
        return results;
    }

    private static void appendVerdictBreakdown(StringBuilder sb,
                                                Map<ProfileMode, List<TranscriptRecorder.RunResult>> grouped) {
        sb.append("## Verdict Breakdown\n\n");
        sb.append("| Layer | Run 1 | Run 2 | Run 3 | Avg Turns |\n");
        sb.append("|---|---|---|---|---|\n");

        for (ProfileMode profile : ProfileMode.values()) {
            var runs = grouped.getOrDefault(profile, List.of());
            sb.append("| ").append(profile.name());
            for (int i = 1; i <= 3; i++) {
                int idx = i;
                var run = runs.stream().filter(r -> r.runNumber() == idx).findFirst();
                sb.append(" | ").append(run.map(r -> r.verdict().name()).orElse("—"));
            }
            double avgTurns = runs.stream().mapToInt(TranscriptRecorder.RunResult::totalTurns).average().orElse(0);
            sb.append(" | ").append(String.format("%.0f", avgTurns)).append(" |\n");
        }
        sb.append("\n");
    }

    private static void appendVerdictGates(StringBuilder sb,
                                            Map<ProfileMode, List<TranscriptRecorder.RunResult>> grouped) {
        sb.append("## Verdict Gates\n\n");
        appendGate(sb, "Layer 1 vs 0 (Jungian vs Baseline)", grouped, ProfileMode.JUNGIAN, ProfileMode.BASELINE);
        appendGate(sb, "Layer 2 vs 0 (Belbin vs Baseline)", grouped, ProfileMode.BELBIN, ProfileMode.BASELINE);
        appendGate(sb, "Layer 3 vs 1 (Composite vs Jungian)", grouped, ProfileMode.COMPOSITE, ProfileMode.JUNGIAN);
        appendGate(sb, "Layer 3 vs 2 (Composite vs Belbin)", grouped, ProfileMode.COMPOSITE, ProfileMode.BELBIN);
    }

    private static void appendGate(StringBuilder sb, String label,
                                    Map<ProfileMode, List<TranscriptRecorder.RunResult>> grouped,
                                    ProfileMode a, ProfileMode b) {
        var runsA = grouped.getOrDefault(a, List.of());
        var runsB = grouped.getOrDefault(b, List.of());
        double avgA = runsA.stream().mapToInt(TranscriptRecorder.RunResult::totalTurns).average().orElse(0);
        double avgB = runsB.stream().mapToInt(TranscriptRecorder.RunResult::totalTurns).average().orElse(0);
        long dawnA = runsA.stream().filter(r -> r.verdict().name().equals("DAWN")).count();
        long dawnB = runsB.stream().filter(r -> r.verdict().name().equals("DAWN")).count();

        sb.append("**").append(label).append(":**\n");
        if (runsA.isEmpty() || runsB.isEmpty()) {
            sb.append("- Insufficient data (").append(runsA.size()).append(" vs ").append(runsB.size()).append(" runs)\n\n");
        } else {
            sb.append("- Avg turns: ").append(String.format("%.0f", avgA)).append(" vs ").append(String.format("%.0f", avgB));
            sb.append(" (delta: ").append(String.format("%+.0f", avgA - avgB)).append(")\n");
            sb.append("- DAWN: ").append(dawnA).append("/").append(runsA.size());
            sb.append(" vs ").append(dawnB).append("/").append(runsB.size()).append("\n\n");
        }
    }
}
