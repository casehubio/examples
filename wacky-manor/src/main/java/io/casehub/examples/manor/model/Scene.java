package io.casehub.examples.manor.model;

import java.util.List;

public record Scene(String id, List<Beat> beats) {
    public Scene {
        beats = List.copyOf(beats);
    }
}
