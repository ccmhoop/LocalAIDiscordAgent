package com.discord.LocalAIDiscordAgent.interactionProcessor;

import com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.service.WebChatMemoryService;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcessToolClient {

    private final WebChatMemoryService webChatMemoryService;

    public ProcessToolClient(WebChatMemoryService webChatMemoryService) {
        this.webChatMemoryService = webChatMemoryService;
    }

    public void saveInteraction (List<Message> messages, UserEntity user){
        webChatMemoryService.save(messages, user);
    }
}
