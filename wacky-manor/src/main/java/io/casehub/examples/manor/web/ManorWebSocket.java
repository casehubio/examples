package io.casehub.examples.manor.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.function.Consumer;

@WebSocket(path = "/ws/manor")
public class ManorWebSocket {

    private static final Logger log = Logger.getLogger(ManorWebSocket.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Inject ManorEventBus eventBus;

    private Consumer<ManorWebSocketEvent> listener;

    @OnOpen
    public void onOpen(WebSocketConnection connection) {
        log.infof("WebSocket client connected: %s", connection.id());
        listener = event -> {
            try {
                connection.sendTextAndAwait(JSON.writeValueAsString(event));
            } catch (Exception e) {
                log.warnf("Failed to send event to %s: %s", connection.id(), e.getMessage());
            }
        };
        eventBus.addListener(listener);
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        log.infof("WebSocket client disconnected: %s", connection.id());
        if (listener != null) {
            eventBus.removeListener(listener);
        }
    }
}
