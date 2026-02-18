package com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.model.GroupChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.repository.GroupChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.chatMemory.service.ChatMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Transactional
    public void saveAndTrim(String conversationId, String username, List<Message> messages) {
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(this.minutesWindow);
        chatRepo.deleteByTimestampBefore(timeWindow);
        chatRepo.deleteAllByUsername(username);
        saveAll(conversationId, username, messages);
        trimDbToMessagesLimit();
    }

    @Override
    public Map<MessageType, List<GroupChatMemory>> sortAndMap(List<GroupChatMemory> memories) {
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(this.minutesWindow);

        if (memories.size() <=2){
            return Collections.emptyMap();
        }

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
    public GroupChatMemory buildChatMemory(String conversationId, String username, Message message) {
        return GroupChatMemory.builder()
                .conversationId(conversationId)
                .username(username)
                .content(message.getText())
                .type(message.getMessageType())
                .timestamp(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();
    }

}