package io.casehub.examples.manor.experiment;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

record EvalFilter(Set<String> characters, Set<String> layers) {

    static EvalFilter from(Optional<String> characters, Optional<String> layers) {
        return new EvalFilter(parse(characters), parse(layers));
    }

    boolean includesCharacter(String agentId) {
        return characters.isEmpty() || characters.contains(agentId);
    }

    boolean includesLayer(String layerKey) {
        return layers.isEmpty() || layers.contains(layerKey);
    }

    private static Set<String> parse(Optional<String> csv) {
        return csv.filter(s -> !s.isBlank())
                  .map(s -> Arrays.stream(s.split(","))
                                  .map(String::trim)
                                  .filter(t -> !t.isEmpty())
                                  .collect(Collectors.toSet()))
                  .orElse(Set.of());
    }
}
