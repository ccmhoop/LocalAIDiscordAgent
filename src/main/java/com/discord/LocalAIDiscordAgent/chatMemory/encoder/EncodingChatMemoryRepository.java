package com.discord.LocalAIDiscordAgent.chatMemory.encoder;

import lombok.NonNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EncodingChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMemoryRepository delegate;

    public EncodingChatMemoryRepository(ChatMemoryRepository delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public void saveAll(@NonNull String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;

        List<Message> toStore = new ArrayList<>(messages.size());

        for (Message m : messages) {
            if (m == null) continue;

            String encoded = ChatMessageContentEncoder.encodeForDb(m);
            if (encoded == null || encoded.isBlank()) continue;

            toStore.add(new ContentOnlyMessage(m, encoded));
        }

        if (!toStore.isEmpty()) {
            delegate.saveAll(conversationId, toStore);
        }
    }

    @Override
    @NonNull
    public List<Message> findByConversationId(@NonNull String conversationId) {
        List<Message> out = delegate.findByConversationId(conversationId);
        return out == null ? List.of() : out;
    }

    @Override
    public void deleteByConversationId(@NonNull String conversationId) {
        delegate.deleteByConversationId(conversationId);
    }

    @Override
    @NonNull
    public List<String> findConversationIds() {
        List<String> out = delegate.findConversationIds();
        return out == null ? List.of() : out;
    }
}
