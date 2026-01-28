package com.discord.LocalAIDiscordAgent.aiAdvisor.filters;

import lombok.NonNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FilteringChatMemory implements ChatMemory {

    private static final int MAX_MESSAGES = 12;

    private static final Pattern FAILURE_MARKERS = Pattern.compile(
            "(?is)\\b(NO_CONTENT|NOT_FOUND|TOOL[_\\s-]?ERROR|TOOL[_\\s-]?FAILED|FUNCTION[_\\s-]?CALL[_\\s-]?FAILED)\\b"
    );

    private static final Pattern INSTRUCTION_MARKERS = Pattern.compile(
            "(?is)\\b(ignore previous|system prompt|you are an ai|act as|follow these rules)\\b"
    );

    private final ChatMemory delegate;

    public FilteringChatMemory(ChatMemory delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate ChatMemory must not be null");
    }

    @Override
    public void add(@NonNull String conversationId, List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }

        List<Message> filtered = messages.stream()
                .filter(Objects::nonNull)
                .filter(this::shouldStore)
                .collect(Collectors.toList());

        if (!filtered.isEmpty()) {
            delegate.add(conversationId, filtered);
        }
    }

    @Override
    @NonNull
    public List<Message> get(@NonNull String conversationId) {
        List<Message> messages = delegate.get(conversationId);

        return messages.stream()
                .filter(Objects::nonNull)
                .filter(this::shouldStore)
                .limit(MAX_MESSAGES)
                .collect(Collectors.toList());
    }

    @Override
    public void clear(@NonNull String conversationId) {
        delegate.clear(conversationId);
    }

    private boolean shouldStore(Message message) {
        MessageType type = message.getMessageType();

        // Never persist system or tool messages
        if (type == MessageType.SYSTEM || type == MessageType.TOOL) {
            return false;
        }

        String text = message.getText();
        if (!StringUtils.hasText(text)) {
            return false;
        }

        String t = text.trim();

        if ("null".equalsIgnoreCase(t)) {
            return false;
        }

        // Prevent assistant self-instruction or hallucinated control text
        if (type == MessageType.ASSISTANT) {
            if (looksLikeFailure(t)) {
                return false;
            }
            if (INSTRUCTION_MARKERS.matcher(t).find()) {
                return false;
            }
        }

        return true;
    }

    private boolean looksLikeFailure(String t) {
        if (t.equalsIgnoreCase("NO_CONTENT") || t.equalsIgnoreCase("NOT_FOUND")) {
            return true;
        }
        if (FAILURE_MARKERS.matcher(t).find()) {
            return true;
        }
        return t.contains("WEBPAGE_FETCH") &&
                t.matches("(?is).*\\bStatus:\\s*ERROR\\b.*");
    }
}
