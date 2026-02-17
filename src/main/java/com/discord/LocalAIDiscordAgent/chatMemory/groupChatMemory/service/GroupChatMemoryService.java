package com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.model.GroupChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.repository.GroupChatMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

@Slf4j
@Service
public class GroupChatMemoryService {

    @Value("${group.chat.memory.message.limit}")
    private int messageLimit;

    @Value("${group.chat.time.window.minutes}")
    private long minutesWindow;

    private final GroupChatMemoryRepository chatRepo;

    public GroupChatMemoryService(GroupChatMemoryRepository groupChatMemoryRepository) {
        this.chatRepo = groupChatMemoryRepository;
    }

    @Transactional
    public void processInteraction(String conversationId, String username, List<Message> messages) {
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(this.minutesWindow);
        chatRepo.deleteByTimestampBefore(timeWindow);
        chatRepo.deleteAllByUsername(username);
        saveAll(conversationId, username, messages);
        trimDbToMessagesLimit();
    }

    public Map<MessageType, List<GroupChatMemory>> getGroupChatToMap() {
        List<GroupChatMemory> memories = chatRepo.findAll();
        if (memories.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(this.minutesWindow);

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

    private List<GroupChatMemory> createSaveAllList(String conversationId, String username, List<Message> messages) {
        return messages.stream().map(m -> buildGroupChatMessages(conversationId, username, m)).toList();
    }

    private GroupChatMemory buildGroupChatMessages(String conversationId, String username, Message message) {
        return GroupChatMemory.builder()
                .conversationId(conversationId)
                .username(username)
                .content(message.getText())
                .type(message.getMessageType())
                .timestamp(LocalDateTime.now())
                .build();
    }

    //----------------------------- DB TRIM TO MEMORY SIZE LIMIT------------------------------------
    private void trimDbToMessagesLimit() {
        List<GroupChatMemory> memories = chatRepo.findAll();
        if (memories.isEmpty()) {
            return;
        }

        int toDeleteIndex = Math.max(0, memories.size() - messageLimit);
        List<GroupChatMemory> deleteMemory = memories.subList(0, toDeleteIndex);

        if (deleteMemory.size() % 2 != 0) {
            chatRepo.deleteAll(deleteMemory.subList(0, deleteMemory.size() - 1));
        } else if (!deleteMemory.isEmpty() || memories.size() > messageLimit) {
            chatRepo.deleteAll(deleteMemory);
        }
    }

}