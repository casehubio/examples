package io.casehub.examples.manor.experiment;

import io.casehub.eidos.api.AgentDisposition;

import java.util.Optional;

public final class DominantFunction {
    private DominantFunction() {}

    public static Optional<String> of(AgentDisposition disposition) {
        if (disposition == null || disposition.dispositionProfile().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(disposition.dispositionProfile().getFirst().term());
    }
}
