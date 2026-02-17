package com.discord.LocalAIDiscordAgent.chatMemory.service;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.model.GroupChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.repository.GroupChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.chatMemory.interfaces.ChatMemoryINTF;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.repository.RecentChatMemoryRepository;
import lombok.Setter;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;

@Setter
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
    public Map<MessageType, List<T>> getChatMemoryAsMap() {
        Assert.isTrue(modelClass.equals(GroupChatMemory.class), "GroupChatMemory is required for this method");
        Assert.isTrue(repo instanceof GroupChatMemoryRepository, "GroupChatMemoryRepository is required for this method");
        List<T> memories = repo.findAll();
        if (memories.isEmpty()) {
            return Collections.emptyMap();
        }
        return sortAndMap(memories);
    }

    public Map<MessageType, List<T>> getChatMemoryAsMap(String username) {
        Assert.isTrue(modelClass.equals(RecentChatMemory.class), "RecentChatMemory is required for this method");
        Assert.isTrue(repo instanceof RecentChatMemoryRepository, "RecentChatMemoryRepository is required for this method");
        List<T> memories = repo.findAll().stream().filter(m -> m.getUsername().equals(username)).toList();
        if (memories.isEmpty()) {
            return Collections.emptyMap();
        }
        return sortAndMap(memories);
    }

    public abstract Map<MessageType, List<T>> sortAndMap(List<T> memories);

    //----------------------------- ABSTRACT SAVE AND TRIM ------------------------------------
    public abstract void saveAndTrim(String conversationId, String username, List<Message> messages);

    //----------------------------- SAVING TO DB  ------------------------------------
    public void saveAll(String conversationId, String username, List<Message> messages) {
        if (messages.size() != 2) {
            return;
        }
        if (messages.getFirst().getMessageType() == ASSISTANT) {
            messages = messages.reversed();
        }
        repo.saveAll(createSaveAllList(conversationId, username, messages));
    }

    private List<T> createSaveAllList(String conversationId, String username, List<Message> messages) {
        return messages.stream().map(m -> buildChatMemory(conversationId, username, m)).toList();
    }

    public abstract T buildChatMemory(String conversationId, String username, Message message);

    //----------------------------- DB TRIM TO ROW LIMIT------------------------------------
    public void trimDbToMessagesLimit() {
        List<T> memories = repo.findAll();
        if (memories.isEmpty()) {
            return;
        }

        int toDeleteIndex = Math.max(0, memories.size() - messageLimit);
        List<T> deleteMemory = memories.subList(0, toDeleteIndex);

        if (deleteMemory.size() % 2 != 0) {
            repo.deleteAll(deleteMemory.subList(0, deleteMemory.size() - 1));
        } else if (!deleteMemory.isEmpty() || memories.size() > messageLimit) {
            repo.deleteAll(deleteMemory);
        }
    }

}
