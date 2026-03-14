package com.discord.LocalAIDiscordAgent.interactionProcessor;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class ProcessChatClient {

    private final RecentChatMemoryService recentService;
    private final GroupChatMemoryService groupService;

    public ProcessChatClient(RecentChatMemoryService recentChatMemoryService, GroupChatMemoryService groupChatMemoryService) {
        this.recentService = recentChatMemoryService;
        this.groupService = groupChatMemoryService;
    }

    public void saveInteraction(List<Message> messages, UserEntity userEntity) {
        recentService.saveAndTrim(messages, userEntity);
        groupService.saveAndTrim(messages, userEntity);
    }

}
