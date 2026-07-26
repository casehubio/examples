package io.casehub.examples.manor.agent;

import io.casehub.qhorus.api.gateway.MessageObserver;
import io.casehub.qhorus.api.gateway.MessageReceivedEvent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class TestMessageObserver implements MessageObserver {

    private final List<CapturedMessage> messages = new CopyOnWriteArrayList<>();

    @Override
    public void onMessage(MessageReceivedEvent event) {
        messages.add(new CapturedMessage(
            event.messageId(),
            event.channelName(),
            event.channelId(),
            event.messageType(),
            event.senderId(),
            event.correlationId(),
            event.content(),
            event.topic()));
    }

    public List<CapturedMessage> messages() {
        return List.copyOf(messages);
    }

    public void clear() {
        messages.clear();
    }
}
