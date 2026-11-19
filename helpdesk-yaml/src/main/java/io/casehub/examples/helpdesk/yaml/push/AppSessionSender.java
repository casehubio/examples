package io.casehub.examples.helpdesk.apps.push;

import io.casehub.pages.push.SessionSender;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@ApplicationScoped
public class AppSessionSender implements SessionSender {

    private final Map<String, Consumer<String>> localHandlers = new ConcurrentHashMap<>();

    @Inject
    ConnectionRegistry registry;

    public void registerLocal(String connectionId, Consumer<String> handler) {
        localHandlers.put(connectionId, handler);
    }

    @Override
    public void send(String connectionId, String message) {
        var local = localHandlers.get(connectionId);
        if (local != null) {
            local.accept(message);
            return;
        }
        WebSocketConnection conn = registry.get(connectionId);
        if (conn != null && !conn.isClosed()) {
            conn.sendTextAndAwait(message);
        }
    }
}
