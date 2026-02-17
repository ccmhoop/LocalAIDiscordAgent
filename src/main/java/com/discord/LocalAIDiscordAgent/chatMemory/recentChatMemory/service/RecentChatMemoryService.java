package com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service;

import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.repository.RecentChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

@Service
public class RecentChatMemoryService {

    @Value("${recent.chat.memory.message.limit}")
    private int messageLimit;

    private final RecentChatMemoryRepository chatRepo;

    public RecentChatMemoryService(RecentChatMemoryRepository recentChatMemoryRepository) {
        this.chatRepo = recentChatMemoryRepository;
    }

    public void processInteraction(String conversationId, String username, List<Message> messages) {
        saveAll(conversationId, username, messages);
        trimToMemorySizeLimit(username);
    }

    public Map<MessageType, List<RecentChatMemory>> sortRecentChatToMap(String username) {
        List<RecentChatMemory> memories = chatRepo.findAllByUsername(username);
        var partitioned = memories.stream()
                .filter(m -> m.getType() == USER || m.getType() == ASSISTANT )
                .collect(Collectors.partitioningBy(
                        m -> m.getType() == USER
                ));
        return Map.of(
                USER, partitioned.get(true),
                ASSISTANT, partitioned.get(false));
    }

    //----------------------------- SAVING TO DB  ------------------------------------
    private void saveAll(String conversationId, String username, List<Message> messages) {
        if (messages.size() != 2) {
            return;
        }
        if (messages.getFirst().getMessageType() == ASSISTANT) {
            messages = messages.reversed();
        }
        chatRepo.saveAll(createSaveAllList(conversationId, username, messages));
    }

    private List<RecentChatMemory> createSaveAllList(String conversationId, String username, List<Message> messages) {
        return messages.stream().map(m -> buildRecentChatMemory(conversationId, username, m)).toList();
    }

    private RecentChatMemory buildRecentChatMemory(String conversationId, String username, Message message) {
        return RecentChatMemory.builder()
                .conversationId(conversationId)
                .username(username)
                .content(message.getText())
                .type(message.getMessageType())
                .timestamp(LocalDateTime.now())
                .build();
    }

    //----------------------------- DB TRIM TO MEMORY SIZE LIMIT------------------------------------
    private void trimToMemorySizeLimit(String username) {
        List<RecentChatMemory> memories = chatRepo.findAllByUsername(username);

        int toDeleteIndex = Math.max(0, memories.size() - messageLimit);
        List<RecentChatMemory> deleteMemory = memories.subList(0, toDeleteIndex);

        if (deleteMemory.size() % 2 != 0) {
            chatRepo.deleteAll(deleteMemory.subList(0, deleteMemory.size() - 1));
        } else if (!deleteMemory.isEmpty() || memories.size() > messageLimit) {
            chatRepo.deleteAll(deleteMemory);
        }
    }
}