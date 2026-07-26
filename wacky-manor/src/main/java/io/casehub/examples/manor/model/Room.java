package io.casehub.examples.manor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Room(
        String id,
        String name,
        String description,
        List<String> adjacentRooms,
        Map<String, GameObject> objects) {

    public Room {
        adjacentRooms = adjacentRooms != null ? List.copyOf(adjacentRooms) : List.of();
        objects = objects != null ? Map.copyOf(objects) : Map.of();
    }
}
