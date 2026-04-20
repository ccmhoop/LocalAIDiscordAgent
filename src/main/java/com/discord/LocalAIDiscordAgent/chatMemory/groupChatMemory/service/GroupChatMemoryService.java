
package com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.model.GroupChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.repository.GroupChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.chatMemory.service.ChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.*;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

@Slf4j
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GroupChatMemoryService extends ChatMemoryService<GroupChatMemory> {

    private static final Comparator<GroupChatMemory> BY_TIME_ASC =
            Comparator.comparing(GroupChatMemory::getTimestamp)
                    .thenComparing(m -> m.getType() == USER ? 0 : 1);

    @Value("${group.chat.time.window.minutes}")
    private long minutesWindow;

    private final GroupChatMemoryRepository chatRepo;
    private DiscGlobalData discGlobalData;

    public GroupChatMemoryService(
            GroupChatMemoryRepository groupChatMemoryRepository,
            @Value("${group.chat.memory.message.limit}") int messageLimit
    ) {
        super(groupChatMemoryRepository, messageLimit, GroupChatMemory.class);
        this.chatRepo = groupChatMemoryRepository;
    }

    public void setDiscGlobalData(DiscGlobalData discGlobalData) {
        this.discGlobalData = discGlobalData;
    }

    public GroupMemory buildMessageMemory() {
        Map<MessageType, List<GroupChatMemory>> sortedGroupMap = getChatMemoryAsMap();

        if (sortedGroupMap.isEmpty()) {
            return null;
        }

        List<GroupChatMemory> users = sortedGroupMap.getOrDefault(USER, Collections.emptyList());
        List<GroupChatMemory> assistants = sortedGroupMap.getOrDefault(ASSISTANT, Collections.emptyList());

        int size = users.size() + assistants.size();
        if (size <= 2) {
            return null;
        }

        Set<UserProfile> participantProfiles = buildParticipantList(users);
        if (participantProfiles.size() <= 2) {
            return null;
        }

        List<GroupMessage> groupMessages = buildSGroupMessages(users, assistants);
        if (groupMessages.isEmpty()) {
            return null;
        }

        return new GroupMemory(participantProfiles, groupMessages);
    }

    @Override
    public void saveAndTrim(List<Message> messages, UserEntity user) {
        try {
            LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(this.minutesWindow);

            List<GroupChatMemory> expiredMemories = new ArrayList<>(
                    chatRepo.findAllByConversationIdAndTimestampBeforeOrderByTimestampAsc(
                            discGlobalData.getGroupConversationId(),
                            timeWindow
                    )
            );

            if (!expiredMemories.isEmpty()) {
                chatRepo.deleteAllInBatch(expiredMemories);
                chatRepo.flush();
            }

            saveAll(messages, user);
            trimDbToMessagesLimit();

            log.debug(
                    "Successfully saved and trimmed group chat memory for group conversation: {}",
                    discGlobalData.getGroupConversationId()
            );
        } catch (Exception e) {
            log.error(
                    "Error in saveAndTrim for group conversation {}: {}",
                    discGlobalData.getGroupConversationId(),
                    e.getMessage(),
                    e
            );
            throw e;
        }
    }

    @Override
    public void trimDbToMessagesLimit() {
        try {
            List<GroupChatMemory> memories = new ArrayList<>(
                    chatRepo.findAllByConversationIdOrderByTimestampAsc(
                            discGlobalData.getGroupConversationId()
                    )
            );

            trimOrderedMemoriesToLimit(memories);
        } catch (Exception e) {
            log.error("Error during group chat memory trimming: {}", e.getMessage(), e);
        }
    }

    @Override
    public Map<MessageType, List<GroupChatMemory>> getChatMemoryAsMap() {
        List<GroupChatMemory> memories = new ArrayList<>(
                chatRepo.findAllByConversationIdOrderByTimestampAsc(
                        discGlobalData.getGroupConversationId()
                )
        );

        if (memories.isEmpty()) {
            return Collections.emptyMap();
        }

        return sortAndMap(memories);
    }

    @Override
    public Map<MessageType, List<GroupChatMemory>> sortAndMap(List<GroupChatMemory> memories) {
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(this.minutesWindow);

        List<GroupChatMemory> ordered = memories.stream()
                .filter(m ->
                        (m.getType() == USER || m.getType() == ASSISTANT)
                                && !m.getTimestamp().isBefore(timeWindow)
                )
                .sorted(BY_TIME_ASC)
                .toList();

        if (ordered.isEmpty()) {
            return Collections.emptyMap();
        }

        List<GroupChatMemory> users = ordered.stream()
                .filter(m -> m.getType() == USER)
                .toList();

        List<GroupChatMemory> assistants = ordered.stream()
                .filter(m -> m.getType() == ASSISTANT)
                .toList();

        if (users.isEmpty() || assistants.isEmpty() || users.size() != assistants.size()) {
            return Collections.emptyMap();
        }

        return Map.of(
                USER, new ArrayList<>(users),
                ASSISTANT, new ArrayList<>(assistants)
        );
    }

    public Set<UserProfile> buildParticipantList(List<GroupChatMemory> users) {
        Set<UserProfile> userProfiles = new LinkedHashSet<>();

        for (GroupChatMemory participant : users) {
            UserEntity user = participant.getUser();
            userProfiles.add(
                    new UserProfile(
                            user.getUserId().toString(),
                            user.getUsername(),
                            user.getServerNickname()
                    )
            );
        }

        return userProfiles;
    }

    public List<GroupMessage> buildSGroupMessages(
            List<GroupChatMemory> users,
            List<GroupChatMemory> assistants
    ) {
        if (users.isEmpty() || assistants.isEmpty()) {
            return Collections.emptyList();
        }

        int minSize = Math.min(users.size(), assistants.size());
        List<GroupMessage> groupMessages = new ArrayList<>(minSize * 2);

        for (int i = 0; i < minSize; i++) {
            GroupChatMemory participant = users.get(i);
            GroupChatMemory assistant = assistants.get(i);

            groupMessages.add(
                    new GroupMessage(
                            participant.getTimestamp().toString(),
                            participant.getUser().getUserId().toString(),
                            "user",
                            participant.getContent()
                    )
            );

            groupMessages.add(
                    new GroupMessage(
                            assistant.getTimestamp().toString(),
                            assistant.getUser().getUserId().toString(),
                            "assistant",
                            assistant.getContent()
                    )
            );
        }

        return groupMessages;
    }

    @Override
    public GroupChatMemory buildChatEntity(Message message, UserEntity user) {
        return GroupChatMemory.builder()
                .guildId(discGlobalData.getGuildId())
                .channelId(discGlobalData.getChannelId())
                .conversationId(discGlobalData.getGroupConversationId())
                .user(user)
                .content(message.getText())
                .type(message.getMessageType())
                .timestamp(LocalDateTime.now())
                .build();
    }

}
