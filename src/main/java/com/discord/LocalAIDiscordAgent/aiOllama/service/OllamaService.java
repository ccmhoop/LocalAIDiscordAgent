package com.discord.LocalAIDiscordAgent.aiOllama.service;

import com.discord.LocalAIDiscordAgent.aiChatClient.systemMsg.AISystemMsg;
import com.discord.LocalAIDiscordAgent.aiMemory.service.AiMemoryContextBuilderService;
import com.discord.LocalAIDiscordAgent.aiMemory.service.MemoryWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class OllamaService {

    private final ChatClient chatClientOllamaKier;
    private final AiMemoryContextBuilderService memoryContextBuilder;

    public OllamaService(ChatClient chatClientOllamaKier, AiMemoryContextBuilderService memoryContextBuilder) {
        this.chatClientOllamaKier = chatClientOllamaKier;
        this.memoryContextBuilder = memoryContextBuilder;
    }

    public String generateKierResponse(String userMessage, String userId, String guildId, String channelId) {
        String conversationId = guildId + ":" + channelId + ":" + userId;

        List<Message> messages = memoryContextBuilder.buildMessages(AISystemMsg.SYSTEM_MESSAGE_KIER, userId, userMessage);

        ChatResponse response = chatClientOllamaKier
                .prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .messages(messages)
                .call()
                .chatResponse();

        if (response == null || Objects.requireNonNull(response.getResult().getOutput().getText()).isBlank()) {
            log.warn("Ollama response was null or blank");
            return "I'm sorry, I didn't understand you.";
        }

        String responseText = response.getResult().getOutput().getText();

        log.info("Ollama response: {}", responseText);

        return responseText;
    }


}
