package com.discord.LocalAIDiscordAgent.chatClient.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OllamaService {

    private final ChatClient scottishChatClient;
    private final ChatClient scottishToolChatClient;

    public OllamaService(
            @Qualifier("scottishChatClient") ChatClient scottishChatClient,
            @Qualifier("scottishToolChatClient") ChatClient scottishToolChatClient
    ) {
        this.scottishChatClient = scottishChatClient;
        this.scottishToolChatClient = scottishToolChatClient;
    }

    public String generateScottishResponse(
            String userMessage,
            String userId,
            String guildId,
            String channelId
    ) {

        String conversationId = buildConversationId(userId, guildId, channelId);
        boolean useTools = shouldUseTools(userMessage);

        ChatClient client = useTools ? scottishToolChatClient : scottishChatClient;

        try {
            ChatResponse chatResponse =
                    client.prompt()
                            .advisors(a ->
                                    a.param(ChatMemory.CONVERSATION_ID, conversationId)
                            )
                            .user(userMessage)
                            .call()
                            .chatResponse();

            String aiResponse = extractChatResponseAsString(chatResponse);

            log.debug(
                    "Ollama response (conversationId={}, useTools={}): {}",
                    conversationId,
                    useTools,
                    aiResponse
            );

            return aiResponse;

        } catch (Exception e) {
            log.error(
                    "Ollama error (conversationId={}, useTools={}): {}",
                    conversationId,
                    useTools,
                    e.getMessage(),
                    e
            );
            return "I had a problem generating a response. Please try again.";
        }
    }

    private String extractChatResponseAsString(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult().getOutput().getText() == null ||
                chatResponse.getResult().getOutput().getText().isBlank()) {
            throw new BlankModelResponseException("Model returned blank output");
        }
        return chatResponse.getResult().getOutput().getText().trim();
    }

    private boolean shouldUseTools(String userMessage) {
        if (userMessage == null) return false;

        String t = userMessage.toLowerCase();

        return t.contains("search online")
                || t.contains("websearch")
                || t.contains("google")
                || t.contains("bing")
                || t.contains("source")
                || t.contains("sources")
                || t.contains("link")
                || t.contains("links")
                || t.contains("http://")
                || t.contains("https://")
                || t.contains("www.")
                || t.contains("lookup")
                || t.contains("look up")
                || t.contains("search");
    }

    private String buildConversationId(String userId, String guildId, String channelId) {
        String safeGuild = (guildId == null || guildId.isBlank()) ? "dm" : guildId;
        String safeChannel = (channelId == null || channelId.isBlank()) ? "dm" : channelId;
        String safeUser = (userId == null || userId.isBlank()) ? "unknown-user" : userId;
        return safeGuild + ":" + safeChannel + ":" + safeUser;
    }

    private static final class BlankModelResponseException extends RuntimeException {
        private BlankModelResponseException(String message) {
            super(message);
        }
    }
}
