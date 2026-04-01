package com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.service;

import com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.groupChatMemory.model.GroupChatMemory;
import com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.groupChatMemory.repository.GroupChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.interfaces.ChatMemoryINTF;
import com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.recentChatMemory.repository.RecentChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
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
    private final DiscGlobalData discGlobalData;


    public ChatMemoryService(JpaRepository<T, Long> repo, int messageLimit, Class<T> modelClass, DiscGlobalData discGlobalData) {
        this.repo = repo;
        this.modelClass = modelClass;
        this.messageLimit = messageLimit;
        this.discGlobalData = discGlobalData;
    }
    public abstract void saveAndTrim( List<Message> messages, UserEntity user);
    public abstract Map<MessageType, List<T>> sortAndMap(List<T> memories);
    public abstract T buildChatEntity(Message message, UserEntity user);
    public abstract Map<MessageType, List<T>> getChatMemoryAsMap();

    //----------------------------- SAVING TO DB  ------------------------------------
    public void saveAll(List<Message> messages, UserEntity user) {
        if (messages == null || messages.isEmpty()) {
            log.warn("Cannot save chat memory: messages list is null or empty");
            return;
        }

        if (messages.size() != 2) {
            log.warn("Cannot save chat memory: expected 2 messages but got {} messages", messages.size());
            return;
        }

        if (messages.getFirst().getMessageType() == ASSISTANT) {
            messages = messages.reversed();
        }

        repo.saveAll(
                createSaveAllList(
                        messages,
                        user
                )
        );

        repo.flush();
    }

    private List<T> createSaveAllList(List<Message> messages, UserEntity user) {
        return messages.stream().map(m -> buildChatEntity( m, user )).collect(Collectors.toCollection(ArrayList::new));
    }

    //----------------------------- DB TRIM TO ROW LIMIT------------------------------------
    public void trimDbToMessagesLimit() {
        List<T> memories = Collections.emptyList();
        try {
            if (repo instanceof RecentChatMemoryRepository && modelClass.equals(RecentChatMemory.class)) {
                memories = repo.findAll().stream().filter(m -> m.getUser().getUserId().toString().equals(discGlobalData.getUserId()) && m.getGuildId().equals(discGlobalData.getGuildId())).toList();
            }else if (repo instanceof GroupChatMemoryRepository && modelClass.equals(GroupChatMemory.class)){
                memories = repo.findAll();
            }

            if (memories.isEmpty() || memories.size() <= messageLimit) {
                return;
            }

            int toDeleteCount = memories.size() - messageLimit;

            if (toDeleteCount % 2 != 0) {
                toDeleteCount = toDeleteCount - 1;
            }

            if (toDeleteCount > 0) {
                List<T> toDelete = memories.subList(0, toDeleteCount);
                repo.deleteAll(toDelete);
                repo.flush();
                log.debug("Trimmed {} chat memory records to maintain limit of {}", toDeleteCount, messageLimit);
            }
        } catch (Exception e) {
            log.error("Error during chat memory trimming: {}", e.getMessage(), e);
        }
    }

}