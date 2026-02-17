package com.discord.LocalAIDiscordAgent.interactionProcessor;

import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcessChatClient {

    private final RecentChatMemoryService recentService;
    private final GroupChatMemoryService groupService;

    public ProcessChatClient(RecentChatMemoryService recentChatMemoryService, GroupChatMemoryService groupChatMemoryService) {
        this.recentService = recentChatMemoryService;
        this.groupService = groupChatMemoryService;
    }

    public void saveInteraction(String conversationId, String username, List<Message> messages){
        recentService.saveAndTrim(conversationId, username, messages);
        groupService.saveAndTrim(conversationId, username, messages);
    }

}
