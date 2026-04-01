package com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.webChatMemory.service;

import com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.webChatMemory.model.WebChatMemory;
import com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.webChatMemory.repository.WebChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
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
    private final DiscGlobalData discGlobalData;

    public WebChatMemoryService(WebChatMemoryRepository webChatMemoryRepository, UserRepository userRepo, DiscGlobalData discGlobalData) {
        this.repo = webChatMemoryRepository;
        this.userRepo = userRepo;
        this.discGlobalData = discGlobalData;
    }

    @Transactional
    public void save(List<Message> messages, UserEntity user){
        repo.deleteAllByUser(user);
        repo.flush();
        List<WebChatMemory> chatMemories = createSaveAllList(messages, user);
        repo.saveAll(chatMemories);
    }

    public Map<MessageType, List<WebChatMemory>> getChatMemoryAsMap() {
        UserEntity userEntity = userRepo.findByUserId(Long.parseLong(discGlobalData.getUserId()));
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

    private List<WebChatMemory> createSaveAllList(List<Message> messages, UserEntity user) {
        return messages.stream().map(m -> buildChatMemory( m, user)).collect(Collectors.toCollection(ArrayList::new));
    }

    private WebChatMemory buildChatMemory(Message message, UserEntity user){
        return WebChatMemory.builder()
                .type(message.getMessageType())
                .user(user)
                .guildId(discGlobalData.getGuildId())
                .content(message.getText())
                .channelId(discGlobalData.getChannelId())
                .timestamp(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .conversationId(discGlobalData.getConversationId())
                .build();
    }

}
