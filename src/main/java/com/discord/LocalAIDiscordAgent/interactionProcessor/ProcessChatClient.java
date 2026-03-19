package com.discord.LocalAIDiscordAgent.interactionProcessor;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import com.discord.LocalAIDiscordAgent.vectorMemory.longTermMemory.LongTermMemoryService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class ProcessChatClient {

    private final GroupChatMemoryService groupService;
    private final RecentChatMemoryService recentService;
    private final LongTermMemoryService longTermMemoryService;

    public ProcessChatClient(
            RecentChatMemoryService recentChatMemoryService,
            GroupChatMemoryService groupChatMemoryService,
            LongTermMemoryService longTermMemoryService
    ) {
        this.recentService = recentChatMemoryService;
        this.groupService = groupChatMemoryService;
        this.longTermMemoryService = longTermMemoryService;
    }

    public void saveInteraction(UserMessage userMessage, AssistantMessage assistantMessage, UserEntity userEntity) {
        List<Message> messages = List.of(userMessage, assistantMessage);
        recentService.saveAndTrim(messages, userEntity);
        groupService.saveAndTrim(messages, userEntity);
        longTermMemoryService.saveLongTermMemory(assistantMessage);
    }

}
