package com.discord.LocalAIDiscordAgent.chatMemory.service;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.model.GroupChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.repository.GroupChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.chatMemory.interfaces.ChatMemoryINTF;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;

@Slf4j
@Setter
@Getter
@Component
public abstract class ChatMemoryService<T extends ChatMemoryINTF> {

    private final JpaRepository<T, Long> repo;
    private final Class<T> modelClass;
    private int messageLimit;

    public ChatMemoryService(
            JpaRepository<T, Long> repo,
            int messageLimit,
            Class<T> modelClass
    ) {
        this.repo = repo;
        this.modelClass = modelClass;
        this.messageLimit = messageLimit;
    }

    public abstract void saveAndTrim(List<Message> messages, UserEntity user);
    public abstract Map<MessageType, List<T>> sortAndMap(List<T> memories);
    public abstract T buildChatEntity(Message message, UserEntity user);
    public abstract Map<MessageType, List<T>> getChatMemoryAsMap();

    public void saveAll(List<Message> messages, UserEntity user) {
        if (messages == null || messages.isEmpty()) {
            log.warn("Cannot save chat memory: messages list is null or empty");
            return;
        }

        if (messages.size() != 2) {
            log.warn("Cannot save chat memory: expected 2 messages but got {}", messages.size());
            return;
        }

        List<Message> orderedMessages = new ArrayList<>(messages);

        if (orderedMessages.getFirst().getMessageType() == ASSISTANT) {
            Collections.reverse(orderedMessages);
        }

        getRepo().saveAll(createSaveAllList(orderedMessages, user));
        getRepo().flush();
    }

    private List<T> createSaveAllList(List<Message> messages, UserEntity user) {
        return messages.stream()
                .map(m -> buildChatEntity(m, user))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Expects memories ordered oldest -> newest.
     */
    protected void trimOrderedMemoriesToLimit(List<T> orderedMemoriesOldestFirst) {
        if (orderedMemoriesOldestFirst == null || orderedMemoriesOldestFirst.isEmpty()) {
            return;
        }

        if (orderedMemoriesOldestFirst.size() <= getMessageLimit()) {
            return;
        }

        int toDeleteCount = orderedMemoriesOldestFirst.size() - getMessageLimit();

        // Keep USER / ASSISTANT pairs aligned.
        if ((toDeleteCount & 1) == 1) {
            toDeleteCount--;
        }

        if (toDeleteCount <= 0) {
            return;
        }

        List<T> toDelete = new ArrayList<>(orderedMemoriesOldestFirst.subList(0, toDeleteCount));

        getRepo().deleteAllInBatch(toDelete);
        getRepo().flush();

        log.debug(
                "Trimmed {} chat memory records to maintain limit of {}",
                toDeleteCount,
                getMessageLimit()
        );
    }

    /**
     * Default implementation kept for non-recent memory types.
     * RecentChatMemoryService should override this with conversation-scoped trimming.
     */
    public void trimDbToMessagesLimit() {
        try {
            if (getRepo() instanceof GroupChatMemoryRepository
                    && getModelClass().equals(GroupChatMemory.class)) {

                List<T> memories = new ArrayList<>(getRepo().findAll());
                trimOrderedMemoriesToLimit(memories);
            }
        } catch (Exception e) {
            log.error("Error during chat memory trimming: {}", e.getMessage(), e);
        }
    }

}