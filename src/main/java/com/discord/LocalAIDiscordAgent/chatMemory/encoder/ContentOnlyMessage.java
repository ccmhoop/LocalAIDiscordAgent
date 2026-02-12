package com.discord.LocalAIDiscordAgent.chatMemory.encoder;

import lombok.NonNull;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class ContentOnlyMessage implements Message {

    private final MessageType messageType;
    private final String text;
    private final Map<String, Object> metadata;

    public ContentOnlyMessage(Message original, String newText) {
        Objects.requireNonNull(original, "original cannot be null");
        this.messageType = Objects.requireNonNull(original.getMessageType(), "messageType cannot be null");
        this.text = (newText == null) ? "" : newText;
        this.metadata = Collections.emptyMap(); // keep it basic
    }

    @Override
    @NonNull
    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
