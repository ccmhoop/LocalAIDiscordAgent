package com.discord.LocalAIDiscordAgent.chatMemory.service;

import com.discord.LocalAIDiscordAgent.chatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.repository.RecentChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RecentChatMemoryService {

    private final RecentChatMemoryRepository chatRepo;

    public RecentChatMemoryService(RecentChatMemoryRepository recentChatMemoryRepository) {
        this.chatRepo = recentChatMemoryRepository;
    }

    public void save(String conversationId, String username, List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;
        List<RecentChatMemory> saveChatList = new ArrayList<>(messages.size());
        for (Message message : messages) {
            saveChatList.add(buildRecentChatMemory(conversationId, username, message));
        }
        chatRepo.saveAll(saveChatList);
    }

    public Map<MessageType, List<RecentChatMemory>> getAllAndSort(String username) {
        List<RecentChatMemory> memories = chatRepo.findAllByUsername(username);

        Map<MessageType, List<RecentChatMemory>> recordMap = new HashMap<>();

        recordMap.put(
                MessageType.USER,
                memories.stream()
                        .filter(m -> Objects.equals(m.getType(), MessageType.USER.toString()))
                        .toList()
        );

        recordMap.put(
                MessageType.ASSISTANT,
                memories.stream()
                        .filter(m -> Objects.equals(m.getType(), MessageType.ASSISTANT.toString()))
                        .toList());

        return recordMap;
    }

    private RecentChatMemory buildRecentChatMemory(String conversationId, String username, Message message) {
        return RecentChatMemory.builder()
                .conversationId(conversationId)
                .username(username)
                .content(message.getText())
                .type(message.getMessageType().toString())
                .timestamp(LocalDateTime.now())
                .build();
    }

}
