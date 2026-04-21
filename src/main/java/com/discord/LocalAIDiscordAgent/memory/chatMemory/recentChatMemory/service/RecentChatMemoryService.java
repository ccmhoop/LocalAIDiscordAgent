package com.discord.LocalAIDiscordAgent.memory.chatMemory.recentChatMemory.service;

import com.discord.LocalAIDiscordAgent.memory.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.memory.chatMemory.recentChatMemory.repository.RecentChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.memory.chatMemory.service.ChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

@Slf4j
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RecentChatMemoryService extends ChatMemoryService<RecentChatMemory> {

    private static final Comparator<RecentChatMemory> BY_TIME_ASC =
            Comparator.comparing(RecentChatMemory::getTimestamp)
                    .thenComparing(m -> m.getType() == USER ? 0 : 1);

    private final RecentChatMemoryRepository chatRepo;
    private DiscGlobalData discGlobalData;

    public RecentChatMemoryService(
            RecentChatMemoryRepository recentChatMemoryRepository,
            @Value("${recent.chat.memory.message.limit}") int messageLimit
    ) {
        super(recentChatMemoryRepository, messageLimit, RecentChatMemory.class);
        this.chatRepo = recentChatMemoryRepository;
    }

    public void setDiscGlobalData(DiscGlobalData discGlobalData) {
        this.discGlobalData = discGlobalData;
    }

    @Override
    public void saveAndTrim(List<Message> messages, UserEntity user) {
        saveAll(messages, user);
        trimDbToMessagesLimit();
    }

    @Override
    public void trimDbToMessagesLimit() {
        try {
            List<RecentChatMemory> memories = new ArrayList<>(
                    chatRepo.findAllByConversationIdOrderByTimestampAsc(
                            discGlobalData.getConversationId()
                    )
            );

            trimOrderedMemoriesToLimit(memories);
        } catch (Exception e) {
            log.error("Error during recent chat memory trimming: {}", e.getMessage(), e);
        }
    }

    public List<RecentMessage> buildMessageMemory() {
        return sortedRecentMessageList();
    }

    private List<RecentMessage> sortedRecentMessageList() {
        Map<MessageType, List<RecentChatMemory>> recentMap = getChatMemoryAsMap();

        if (recentMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecentChatMemory> userMessages =
                recentMap.getOrDefault(USER, Collections.emptyList());

        List<RecentChatMemory> assistantMessages =
                recentMap.getOrDefault(ASSISTANT, Collections.emptyList());

        int size = Math.min(userMessages.size(), assistantMessages.size());

        if (size == 0) {
            return Collections.emptyList();
        }

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

    private static List<RecentMessage> orderAndBuildRecentMessages(
            int size,
            List<RecentChatMemory> userMessages,
            List<RecentChatMemory> assistantMessages
    ) {
        List<RecentMessage> recentMessages = new ArrayList<>(size * 2);

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
        List<RecentChatMemory> memories = new ArrayList<>(
                chatRepo.findAllByConversationIdOrderByTimestampAsc(
                        discGlobalData.getConversationId()
                )
        );

        if (memories.isEmpty()) {
            return Collections.emptyMap();
        }

        return sortAndMap(memories);
    }

    @Override
    public Map<MessageType, List<RecentChatMemory>> sortAndMap(List<RecentChatMemory> memories) {
        List<RecentChatMemory> ordered = memories.stream()
                .filter(m -> m.getType() == USER || m.getType() == ASSISTANT)
                .sorted(BY_TIME_ASC)
                .toList();

        if (ordered.isEmpty()) {
            return Collections.emptyMap();
        }

        List<RecentChatMemory> userMessages = ordered.stream()
                .filter(m -> m.getType() == USER)
                .toList();

        List<RecentChatMemory> assistantMessages = ordered.stream()
                .filter(m -> m.getType() == ASSISTANT)
                .toList();

        if (userMessages.isEmpty() || assistantMessages.isEmpty()) {
            return Collections.emptyMap();
        }

        return Map.of(
                USER, new ArrayList<>(userMessages),
                ASSISTANT, new ArrayList<>(assistantMessages)
        );
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
                .timestamp(LocalDateTime.now())
                .build();
    }
}