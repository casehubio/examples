package io.casehub.examples.manor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameObject(
        String id,
        String name,
        String description,
        double x,
        Set<String> visibleTo,
        boolean portable,
        boolean interactable,
        String interactionRequires,
        String yields,
        List<String> usableWith,
        String itemId) {

    public GameObject {
        visibleTo  = visibleTo != null ? Set.copyOf(visibleTo) : Set.of();
        usableWith = usableWith != null ? List.copyOf(usableWith) : List.of();
        if (itemId == null) {itemId = id;}
    }

    public boolean isVisibleToAll() {
        return visibleTo.isEmpty();
    }
}
