package io.casehub.examples.manor.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class FormationMemoryParser {

    private static final Pattern ANNOTATION_PATTERN =
            Pattern.compile("\\{\\{serves:\\s*([\\w-]+)\\|(\\w+):\\s*(.*?)\\}\\}");

    private FormationMemoryParser() {}

    public static List<SocialConfig.FormationMemory.ConcernAnnotation> extractAnnotations(String episodeText) {
        var matcher = ANNOTATION_PATTERN.matcher(episodeText);
        var annotations = new ArrayList<SocialConfig.FormationMemory.ConcernAnnotation>();
        while (matcher.find()) {
            annotations.add(new SocialConfig.FormationMemory.ConcernAnnotation(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3).strip(),
                    matcher.start(),
                    matcher.end()));
        }
        return List.copyOf(annotations);
    }

    public static String stripAnnotations(String episodeText) {
        return ANNOTATION_PATTERN.matcher(episodeText).replaceAll("$3").strip();
    }

    public static SocialConfig.FormationMemory.ConcernAnnotation parseWholeMemory(String serves, String episodeText) {
        var parts = serves.split("\\|", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid serves format, expected 'drive|degree': " + serves);
        }
        return new SocialConfig.FormationMemory.ConcernAnnotation(
                parts[0].strip(), parts[1].strip(), episodeText.strip(), 0, episodeText.length());
    }
}
