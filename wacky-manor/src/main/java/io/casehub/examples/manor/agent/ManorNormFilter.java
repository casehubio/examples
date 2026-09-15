package io.casehub.examples.manor.agent;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class ManorNormFilter {

    static final int CONTEXT_BOOST = 10;
    static final int MIN_WORD_LENGTH = 3;

    private ManorNormFilter() {}

    public static List<SocialConfig.NormEntry> score(
            List<SocialConfig.NormEntry> allNorms,
            Collection<String> activeCognitiveSubjects,
            Collection<String> activeInventory,
            int maxNorms) {

        Set<String> matchWords = extractMatchWords(activeCognitiveSubjects);
        Set<String> inventoryWords = extractMatchWords(activeInventory);

        record Scored(SocialConfig.NormEntry norm, int score) {}

        return allNorms.stream()
                .map(norm -> {
                    String ruleLower = norm.rule().toLowerCase(Locale.ROOT);
                    int score = norm.priority();
                    if (matchWords.stream().anyMatch(ruleLower::contains)) {
                        score += CONTEXT_BOOST;
                    }
                    if (inventoryWords.stream().anyMatch(ruleLower::contains)) {
                        score += CONTEXT_BOOST;
                    }
                    return new Scored(norm, score);
                })
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .limit(maxNorms)
                .map(Scored::norm)
                .toList();
    }

    private static Set<String> extractMatchWords(Collection<String> names) {
        return names.stream()
                .flatMap(name -> java.util.Arrays.stream(name.split("\\s+")))
                .filter(word -> word.length() >= MIN_WORD_LENGTH)
                .map(word -> word.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
