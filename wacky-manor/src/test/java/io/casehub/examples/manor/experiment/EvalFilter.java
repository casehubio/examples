package io.casehub.examples.manor.experiment;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

record EvalFilter(Set<String> characters, Set<String> layers,
                  Set<String> briefings, Set<String> mechanisms) {

    static EvalFilter from(Optional<String> characters, Optional<String> layers) {
        return new EvalFilter(parse(characters), parse(layers), Set.of(), Set.of());
    }

    static EvalFilter from(Optional<String> characters, Optional<String> layers,
                           Optional<String> briefings, Optional<String> mechanisms) {
        return new EvalFilter(parse(characters), parse(layers),
                              parse(briefings), parse(mechanisms));
    }

    boolean includesCharacter(String agentId) {
        return characters.isEmpty() || characters.contains(agentId);
    }

    boolean includesLayer(String layerKey) {
        return layers.isEmpty() || layers.contains(layerKey);
    }

    boolean includesBriefing(String briefingKey) {
        return briefings.isEmpty() || briefings.contains(briefingKey);
    }

    boolean includesMechanism(String mechanismKey) {
        return mechanisms.isEmpty() || mechanisms.contains(mechanismKey);
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
