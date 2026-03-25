
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

@Slf4j
@Service
public class GroupChatMemoryService extends ChatMemoryService<GroupChatMemory> {

    @Value("${group.chat.time.window.minutes}")
    private long minutesWindow;

    private final GroupChatMemoryRepository chatRepo;
    private final DiscGlobalData discGlobalData;

    public GroupChatMemoryService(GroupChatMemoryRepository groupChatMemoryRepository, @Value("${group.chat.memory.message.limit}") int messageLimit, DiscGlobalData discGlobalData) {
        super(groupChatMemoryRepository, messageLimit, GroupChatMemory.class, discGlobalData);
        this.chatRepo = groupChatMemoryRepository;
        this.discGlobalData = discGlobalData;
    }

    public GroupMemory buildMessageMemory()  {
        Map<MessageType, List<GroupChatMemory>> sortedGroupMap = getChatMemoryAsMap();
        if (sortedGroupMap.isEmpty()) {
            return null;
        }
        List<GroupChatMemory> users = sortedGroupMap.getOrDefault(USER, List.of());
        List<GroupChatMemory> assistants = sortedGroupMap.getOrDefault(ASSISTANT, List.of());
        int size = users.size() + assistants.size();
        if (size <= 2 ) {
            return null;
        }

        Set<UserProfile> participantProfiles = buildParticipantList(users);

        if (participantProfiles.size() <=2) {
            return null;
        }

        List<GroupMessage> groupMessages = buildSGroupMessages(users, assistants);

        return new GroupMemory(participantProfiles, groupMessages);
    }

    @Override
    public void saveAndTrim( List<Message> messages, UserEntity user) {

        try {
            LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(this.minutesWindow);
            List<GroupChatMemory> memories = chatRepo.findAllByGuildId(discGlobalData.getGuildId()).stream()
                    .filter(m -> LocalDateTime.now().isBefore(timeWindow) || m.getUser().getUserId().toString().equals(discGlobalData.getUserId())
                    ).toList();

            chatRepo.deleteAll(memories);
            chatRepo.flush();

            saveAll(messages, user);
            chatRepo.flush();

            trimDbToMessagesLimit();
            log.debug("Successfully saved and trimmed group chat memory for user: {}", discGlobalData.getUsername());
        } catch (Exception e) {
            log.error("Error in saveAndTrim for user {}: {}", discGlobalData.getUsername(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Map<MessageType, List<GroupChatMemory>> getChatMemoryAsMap() {
        List<GroupChatMemory> memories = chatRepo.findAll().stream().filter(m -> m.getConversationId().equals(discGlobalData.getGroupConversationId())).collect(Collectors.toList());
//        if (memories.isEmpty() || memories.size() <= 2) {
//            return Collections.emptyMap();
//        }
        if (memories.isEmpty()) {
            return Collections.emptyMap();
        }
        return sortAndMap(memories);
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

        if (partitioned.get(true).isEmpty() || partitioned.get(false).isEmpty() ||
                partitioned.get(true).size() != partitioned.get(false).size()) {
            return Collections.emptyMap();
        }

        return Map.of(
                USER, partitioned.get(true),
                ASSISTANT, partitioned.get(false));
    }

    public Set<UserProfile> buildParticipantList(List<GroupChatMemory> users) {
        Set<UserProfile> userProfiles = new HashSet<>();
        for (GroupChatMemory participant  : users) {
            UserEntity user = participant.getUser();
            userProfiles.add(new UserProfile(user.getUserId().toString(), user.getUsername(), user.getServerNickname()));
        }
         return userProfiles;
    }

    public List<GroupMessage> buildSGroupMessages(List<GroupChatMemory> users, List<GroupChatMemory> assistants) {

        if (users.isEmpty() || assistants.isEmpty()) {
            return null;
        }

        // Use the minimum size of both lists to avoid IndexOutOfBoundsException
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
                .timestamp(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();
    }

}
