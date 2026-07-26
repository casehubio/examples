package io.casehub.examples.manor.agent;

import io.casehub.qhorus.api.message.MessageType;

import java.util.UUID;

record CapturedMessage(
        Long messageId,
        String channelName,
        UUID channelId,
        MessageType messageType,
        String senderId,
        String correlationId,
        String content,
        String topic) {}
