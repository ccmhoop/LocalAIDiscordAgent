package com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service;

import com.discord.LocalAIDiscordAgent.chatClient.helpers.ChatClientHelpers;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.repository.RecentChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.chatMemory.service.ChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.user.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey.*;
import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

@Slf4j
@Service
public class RecentChatMemoryService extends ChatMemoryService<RecentChatMemory>  {

    public RecentChatMemoryService(
            RecentChatMemoryRepository recentChatMemoryRepository,
            @Value("${recent.chat.memory.message.limit}") int messageLimit) {
        super(recentChatMemoryRepository, messageLimit, RecentChatMemory.class);
    }

    @Override
    public void saveAndTrim(Map<DiscDataKey, String> discDataMap, List<Message> messages, UserEntity user) {
        saveAll(discDataMap, messages, user);
        trimDbToMessagesLimit(discDataMap);
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
    public RecentChatMemory buildChatMemory(Map<DiscDataKey, String> discDataMap, Message message, UserEntity user) {
        if (discDataMap == null || message == null) {
            throw new IllegalArgumentException("discDataMap and message cannot be null");
        }

        return RecentChatMemory.builder()
                .guildId(discDataMap.get(GUILD_ID))
                .channelId(discDataMap.get(CHANNEL_ID))
                .conversationId(ChatClientHelpers.buildConversationId(discDataMap))
                .user(user)
                .content(message.getText())
                .type(message.getMessageType())
                .timestamp(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();
    }


}
