
package com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service;

import com.discord.LocalAIDiscordAgent.chatClient.helpers.ChatClientHelpers;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.model.GroupChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.repository.GroupChatMemoryRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey.*;
import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

@Slf4j
@Service
public class GroupChatMemoryService extends ChatMemoryService<GroupChatMemory> {

    @Value("${group.chat.time.window.minutes}")
    private long minutesWindow;

    private final GroupChatMemoryRepository chatRepo;

    public GroupChatMemoryService(GroupChatMemoryRepository groupChatMemoryRepository, @Value("${group.chat.memory.message.limit}") int messageLimit) {
        super(groupChatMemoryRepository, messageLimit, GroupChatMemory.class);
        this.chatRepo = groupChatMemoryRepository;
    }

    @Override
    public void saveAndTrim(Map<DiscDataKey, String> discDataMap, List<Message> messages, UserEntity user) {
        if (discDataMap == null || discDataMap.get(USERNAME) == null) {
            log.warn("Cannot save and trim group chat memory: discDataMap or username is null");
            return;
        }

        try {
            LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(this.minutesWindow);
            List<GroupChatMemory> memories = chatRepo.findAllByGuildId(discDataMap.get(GUILD_ID)).stream()
                    .filter(m -> LocalDateTime.now().isBefore(timeWindow) || m.getUser().getUserId().toString().equals(discDataMap.get(USER_ID))
                    ).toList();

            chatRepo.deleteAll(memories);
            chatRepo.flush();

            saveAll(discDataMap, messages, user);
            chatRepo.flush();

            trimDbToMessagesLimit(discDataMap);
            log.debug("Successfully saved and trimmed group chat memory for user: {}", discDataMap.get(USERNAME));
        } catch (Exception e) {
            log.error("Error in saveAndTrim for user {}: {}", discDataMap.get(USERNAME), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Map<MessageType, List<GroupChatMemory>> sortAndMap(List<GroupChatMemory> memories) {
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(this.minutesWindow);

//        if (memories.size() <= 2){
//            return Collections.emptyMap();
//        }

        var partitioned = memories.stream()
                .filter(m -> (m.getType() == USER || m.getType() == ASSISTANT) && m.getTimestamp().isAfter(timeWindow))
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
    public GroupChatMemory buildChatMemory(Map<DiscDataKey, String> discDataMap, Message message, UserEntity user) {
        if (discDataMap == null || message == null) {
            throw new IllegalArgumentException("discDataMap and message cannot be null");
        }

        return GroupChatMemory.builder()
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
