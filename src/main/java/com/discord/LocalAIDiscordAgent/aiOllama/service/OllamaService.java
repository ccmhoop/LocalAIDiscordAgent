package com.discord.LocalAIDiscordAgent.aiOllama.service;

import com.discord.LocalAIDiscordAgent.aiChatClient.systemMsg.AISystemMsg;
import com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.service.AiMemoryContextBuilderService;
import com.discord.LocalAIDiscordAgent.aiTools.websearch.AIWebFilterTool;
import com.discord.LocalAIDiscordAgent.aiTools.websearch.AIWebSearchTool;
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

    private final ChatClient scottishChatClient;
    private final AiMemoryContextBuilderService memoryContextBuilder;
    private final List<Object> aiToolsConfig;

    public OllamaService(ChatClient chatClientOllamaScottish, AiMemoryContextBuilderService memoryContextBuilder, List<Object> aiToolsConfig) {
        this.scottishChatClient = chatClientOllamaScottish;
        this.memoryContextBuilder = memoryContextBuilder;
        this.aiToolsConfig = aiToolsConfig;
    }

    public String generateScottishResponse(String userMessage, String userId, String guildId, String channelId) {
        String conversationId =  userId;

        List<Message> messages = memoryContextBuilder.buildMessages(AISystemMsg.SYSTEM_MESSAGE_SCOTTISH_AGENT, userId, userMessage);

        System.out.println(messages.toString());

        ChatResponse response = scottishChatClient
                .prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(aiToolsConfig)
                .messages(messages)
                .call()
                .chatResponse();


        System.out.println(response.toString());

        if (response.getResult().getOutput().getText() == null || response.getResult().getOutput().getText().isBlank()) {
            log.warn("Ollama response was null or blank");
            return "I'm sorry, I didn't understand you.";
        }

        String responseText = response.getResult().getOutput().getText();

        log.info("Ollama response: {}", responseText);

        return responseText;
    }

}
