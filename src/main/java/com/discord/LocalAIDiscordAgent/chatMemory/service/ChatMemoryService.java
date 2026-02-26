package com.discord.LocalAIDiscordAgent.chatMemory.service;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.model.GroupChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.repository.GroupChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.chatMemory.interfaces.ChatMemoryINTF;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.repository.RecentChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.user.UserEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

import static com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey.GUILD_ID;
import static com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey.USER_ID;
import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;

@Slf4j
@Setter
@Getter
@Component
public abstract class ChatMemoryService<T extends ChatMemoryINTF> {

    private final JpaRepository<T, Long> repo;
    private final Class<T> modelClass;

    @Setter
    private int messageLimit;

    public ChatMemoryService(JpaRepository<T, Long> repo, int messageLimit, Class<T> modelClass) {
        this.repo = repo;
        this.modelClass = modelClass;
        this.messageLimit = messageLimit;
    }

    //----------------------------- GET CHAT MEMORY FROM DB  ------------------------------------
    public Map<MessageType, List<T>> getChatMemoryAsMap(String guildId) {
        Assert.isTrue(modelClass.equals(GroupChatMemory.class), "GroupChatMemory is required for this method");
        Assert.isTrue(repo instanceof GroupChatMemoryRepository, "GroupChatMemoryRepository is required for this method");
        List<T> memories = repo.findAll().stream().filter(m -> m.getGuildId().equals(guildId)).collect(Collectors.toList());
        if (memories.isEmpty()) {
            return Collections.emptyMap();
        }
        return sortAndMap(memories);
    }

    public Map<MessageType, List<T>> getChatMemoryAsMap(String guildId ,String userId) {
        Assert.isTrue(modelClass.equals(RecentChatMemory.class), "RecentChatMemory is required for this method");
        Assert.isTrue(repo instanceof RecentChatMemoryRepository, "RecentChatMemoryRepository is required for this method");
        List<T> memories = repo.findAll().stream().filter(m ->  m.getGuildId().equals(guildId) && m.getUser().getUserId().toString().equals(userId)).collect(Collectors.toList());
        if (memories.isEmpty()) {
            return Collections.emptyMap();
        }
        return sortAndMap(memories);
    }

    public abstract Map<MessageType, List<T>> sortAndMap(List<T> memories);

    //----------------------------- ABSTRACT SAVE AND TRIM ------------------------------------
    public abstract void saveAndTrim(Map<DiscDataKey, String> discDataMap, List<Message> messages, UserEntity user);

    //----------------------------- SAVING TO DB  ------------------------------------
    public void saveAll(Map<DiscDataKey, String> discDataMap, List<Message> messages, UserEntity user) {
        if (messages == null || messages.isEmpty()) {
            log.warn("Cannot save chat memory: messages list is null or empty");
            return;
        }

        if (discDataMap == null || discDataMap.isEmpty()) {
            log.warn("Cannot save chat memory: discDataMap is null or empty");
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
                        discDataMap,
                        messages,
                        user
                )
        );

        repo.flush();
    }


    private List<T> createSaveAllList(Map<DiscDataKey, String> discDataMap, List<Message> messages, UserEntity user) {
        return messages.stream().map(m -> buildChatMemory(discDataMap, m, user )).collect(Collectors.toCollection(ArrayList::new));
    }

    public abstract T buildChatMemory(Map<DiscDataKey, String> discDataMap, Message message, UserEntity user);

    //----------------------------- DB TRIM TO ROW LIMIT------------------------------------
    public void trimDbToMessagesLimit(Map<DiscDataKey, String> discDataMap) {
        List<T> memories = Collections.emptyList();
        try {
            if (repo instanceof RecentChatMemoryRepository && modelClass.equals(RecentChatMemory.class)) {
                memories = repo.findAll().stream().filter(m -> m.getUser().getUserId().toString().equals(discDataMap.get(USER_ID)) && m.getGuildId().equals(discDataMap.get(GUILD_ID))).toList();
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