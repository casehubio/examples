package io.casehub.examples.manor.agent;

import java.util.Collection;
import java.util.List;

public final class ManorNormFilter {

    private ManorNormFilter() {}

    public static List<SocialConfig.NormEntry> filter(
            List<SocialConfig.NormEntry> allNorms,
            Collection<String> nearbyCharacterNames,
            Collection<String> inventory) {
        return allNorms.stream()
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .toList();
    }
}
