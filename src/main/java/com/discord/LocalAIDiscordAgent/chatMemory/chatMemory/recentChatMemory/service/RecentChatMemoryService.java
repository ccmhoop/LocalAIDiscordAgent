package com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.recentChatMemory.service;

import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.recentChatMemory.repository.RecentChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.service.ChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class RecentChatMemoryService extends ChatMemoryService<RecentChatMemory> {

    private final RecentChatMemoryRepository chatRepo;
    private final DiscGlobalData discGlobalData;

    public RecentChatMemoryService(RecentChatMemoryRepository recentChatMemoryRepository,
                                   @Value("${recent.chat.memory.message.limit}") int messageLimit, DiscGlobalData discGlobalData) {
        super(recentChatMemoryRepository, messageLimit, RecentChatMemory.class, discGlobalData);
        this.chatRepo = recentChatMemoryRepository;
        this.discGlobalData = discGlobalData;
    }

    @Override
    public void saveAndTrim( List<Message> messages, UserEntity user) {
        saveAll( messages, user);
        trimDbToMessagesLimit();
    }

    public List<RecentMessage> buildMessageMemory() {
        List<RecentMessage> recentMessages = sortedRecentMessageList();
        if (recentMessages.isEmpty()) {
            return null;
        }
        return new ArrayList<>(recentMessages);
    }

    private List<RecentMessage> sortedRecentMessageList() {
        Map<MessageType, List<RecentChatMemory>> recentMap = getChatMemoryAsMap();

        if (recentMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecentChatMemory> userMessages = recentMap.get(USER);
        List<RecentChatMemory> assistantMessages = recentMap.get(ASSISTANT);

        int size = Math.min(userMessages.size(), assistantMessages.size());

        return orderAndBuildRecentMessages(size, userMessages, assistantMessages);
    }

    public static List<RecentMessage> reduceAndBuildRecentMessages(
            List<RecentChatMemory> assistantMessages,
            List<RecentChatMemory> userMessages,
            List<Integer> indexes
    ) {
        int userSize = userMessages.size();
        int assistantSize = assistantMessages.size();
        List<RecentMessage> recentMessages = new ArrayList<>();

        for (int i : indexes) {
            if (i < userSize) {
                RecentChatMemory user = userMessages.get(i);
                recentMessages.add(new RecentMessage(
                        user.getTimestamp().toString(),
                        MessageType.USER.toString(),
                        user.getContent()
                ));
            }
            if (i < assistantSize) {
                RecentChatMemory assistant = assistantMessages.get(i);
                recentMessages.add(new RecentMessage(
                        assistant.getTimestamp().toString(),
                        MessageType.ASSISTANT.toString(),
                        assistant.getContent()
                ));
            }
        }
        return recentMessages;
    }

    private static List<RecentMessage> orderAndBuildRecentMessages(int size, List<RecentChatMemory> userMessages, List<RecentChatMemory> assistantMessages) {
        List<RecentMessage> recentMessages = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            RecentChatMemory user = userMessages.get(i);
            RecentChatMemory assistant = assistantMessages.get(i);

            recentMessages.add(new RecentMessage(
                    user.getTimestamp().toString(),
                    MessageType.USER.toString(),
                    user.getContent()
            ));

            recentMessages.add(new RecentMessage(
                    assistant.getTimestamp().toString(),
                    MessageType.ASSISTANT.toString(),
                    assistant.getContent()
            ));
        }
        return recentMessages;
    }

    @Override
    public Map<MessageType, List<RecentChatMemory>> getChatMemoryAsMap() {
        List<RecentChatMemory> memories = new ArrayList<>(chatRepo.findAllByConversationId(discGlobalData.getConversationId()));
        if (memories.isEmpty()) {
            return Collections.emptyMap();
        }
        return sortAndMap(memories);
    }

    @Override
    public Map<MessageType, List<RecentChatMemory>> sortAndMap(List<RecentChatMemory> memories) {
        var partitioned = memories.stream()
                .filter(m -> m.getType() == USER || m.getType() == ASSISTANT)
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
    public RecentChatMemory buildChatEntity(Message message, UserEntity user) {
        return RecentChatMemory.builder()
                .guildId(discGlobalData.getGuildId())
                .channelId(discGlobalData.getChannelId())
                .conversationId(discGlobalData.getConversationId())
                .user(user)
                .content(message.getText())
                .type(message.getMessageType())
                .timestamp(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();
    }

}
