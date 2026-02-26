package com.discord.LocalAIDiscordAgent.interactionProcessor;

import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.user.UserEntity;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
public class ProcessChatClient {

    private final RecentChatMemoryService recentService;
    private final GroupChatMemoryService groupService;

    public ProcessChatClient(RecentChatMemoryService recentChatMemoryService, GroupChatMemoryService groupChatMemoryService) {
        this.recentService = recentChatMemoryService;
        this.groupService = groupChatMemoryService;
    }

    public void saveInteraction(Map<DiscDataKey, String> discDataMap, List<Message> messages, UserEntity userEntity){
        recentService.saveAndTrim(discDataMap, messages, userEntity);
        groupService.saveAndTrim(discDataMap, messages, userEntity);
    }

}
