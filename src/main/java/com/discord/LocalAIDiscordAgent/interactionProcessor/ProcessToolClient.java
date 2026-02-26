package com.discord.LocalAIDiscordAgent.interactionProcessor;

import com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.service.WebChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.user.UserEntity;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProcessToolClient {

    private final WebChatMemoryService webChatMemoryService;

    public ProcessToolClient(WebChatMemoryService webChatMemoryService) {
        this.webChatMemoryService = webChatMemoryService;
    }

    public void saveInteraction (Map<DiscDataKey, String> discDataMap, List<Message> messages, UserEntity user){
        webChatMemoryService.save(discDataMap, messages, user);
    }
}
