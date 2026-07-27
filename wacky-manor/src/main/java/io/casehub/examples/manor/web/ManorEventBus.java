package io.casehub.examples.manor.web;

import io.casehub.examples.manor.engine.WorldState;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@ApplicationScoped
public class ManorEventBus {

    private final    List<Consumer<ManorWebSocketEvent>> listeners = new CopyOnWriteArrayList<>();
    private volatile WorldState                          activeWorld;

    public void setActiveWorld(WorldState world) {
        this.activeWorld = world;
    }

    public void addListener(Consumer<ManorWebSocketEvent> listener) {
        listeners.add(listener);
        if (activeWorld != null) {
            listener.accept(buildSnapshot(activeWorld));
        }
    }

    public void removeListener(Consumer<ManorWebSocketEvent> listener) {
        listeners.remove(listener);
    }

    public void broadcast(ManorWebSocketEvent event) {
        for (var listener : listeners) {
            listener.accept(event);
        }
    }

    public ManorWebSocketEvent buildSnapshot(WorldState world) {
        var characters = world.characters().values().stream()
                              .map(ManorWebSocketEvent.CharacterSnapshot::from)
                              .toList();

        var rooms = world.rooms().values().stream()
                         .map(room -> new ManorWebSocketEvent.RoomSnapshot(
                                 room.id(), room.name(),
                                 room.objects().values().stream()
                                     .map(obj -> new ManorWebSocketEvent.RoomSnapshot.ObjectSnapshot(
                                             obj.id(), obj.name(), obj.x()))
                                     .toList()))
                         .toList();

        return ManorWebSocketEvent.snapshot(characters, rooms);
    }
}
