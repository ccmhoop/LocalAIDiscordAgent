package com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service;

import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.repository.RecentChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.chatMemory.service.ChatMemoryService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

@Service
public class RecentChatMemoryService extends ChatMemoryService<RecentChatMemory>  {

    public RecentChatMemoryService(
            RecentChatMemoryRepository recentChatMemoryRepository,
            @Value("${recent.chat.memory.message.limit}") int messageLimit) {
        super(recentChatMemoryRepository, messageLimit, RecentChatMemory.class);
    }

    @Override
    public void saveAndTrim(String conversationId, String username, List<Message> messages) {
        saveAll(conversationId, username, messages);
        trimDbToMessagesLimit();
    }

    @Override
    public Map<MessageType, List<RecentChatMemory>> sortAndMap(List<RecentChatMemory> memories) {
        var partitioned = memories.stream()
                .filter(m -> m.getType() == USER || m.getType() == ASSISTANT )
                .collect(Collectors.partitioningBy(
                        m -> m.getType() == USER
                ));

        if (partitioned.get(true).isEmpty() || partitioned.get(false).isEmpty()) {
            return Collections.emptyMap();
        }

        return Map.of(
                USER, partitioned.get(true),
                ASSISTANT, partitioned.get(false));
    }

    @Override
    public RecentChatMemory buildChatMemory(String conversationId, String username, Message message) {
        return RecentChatMemory.builder()
                .conversationId(conversationId)
                .username(username)
                .content(message.getText())
                .type(message.getMessageType())
                .timestamp(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();
    }

}