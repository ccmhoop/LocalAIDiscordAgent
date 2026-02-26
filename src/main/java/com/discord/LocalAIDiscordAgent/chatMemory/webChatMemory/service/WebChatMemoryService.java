package com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.service;

import com.discord.LocalAIDiscordAgent.chatClient.helpers.ChatClientHelpers;
import com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.model.WebChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.repository.WebChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.user.UserEntity;
import com.discord.LocalAIDiscordAgent.user.repository.UserRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

@Service
public class WebChatMemoryService {

    private final WebChatMemoryRepository repo;
    private final UserRepository userRepo;

    public WebChatMemoryService(WebChatMemoryRepository webChatMemoryRepository, UserRepository userRepo) {
        this.repo = webChatMemoryRepository;
        this.userRepo = userRepo;
    }

    @Transactional
    public void save(Map<DiscDataKey, String> discDataMap, List<Message> messages, UserEntity user){
        String userId = discDataMap.get(DiscDataKey.USER_ID);
        repo.deleteAllByUser(user);
        repo.flush();
        List<WebChatMemory> chatMemories = createSaveAllList(discDataMap, messages, user);
        repo.saveAll(chatMemories);
    }

    public Map<MessageType, List<WebChatMemory>> getChatMemoryAsMap(String userId) {
        UserEntity userEntity = userRepo.findByUserId(Long.parseLong(userId));
        var partitioned = repo.findAllByUser(userEntity).stream()
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

    private List<WebChatMemory> createSaveAllList(Map<DiscDataKey, String> discDataMap, List<Message> messages, UserEntity user) {
        return messages.stream().map(m -> buildChatMemory(discDataMap, m, user)).collect(Collectors.toCollection(ArrayList::new));
    }

    private WebChatMemory buildChatMemory(Map<DiscDataKey, String> discDataMap, Message message, UserEntity user){
        return WebChatMemory.builder()
                .type(message.getMessageType())
                .user(user)
                .guildId(discDataMap.get(DiscDataKey.GUILD_ID))
                .content(message.getText())
                .channelId(discDataMap.get(DiscDataKey.CHANNEL_ID))
                .timestamp(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .conversationId(ChatClientHelpers.buildConversationId(discDataMap))
                .build();
    }

}
