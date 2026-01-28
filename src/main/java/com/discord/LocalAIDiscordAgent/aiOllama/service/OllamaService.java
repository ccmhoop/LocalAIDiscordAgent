package com.discord.LocalAIDiscordAgent.aiOllama.service;

import com.discord.LocalAIDiscordAgent.aiSystemMsgBuilder.service.SystemMsgService;
import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.service.WebSearchMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class OllamaService {

    private final ChatClient scottishChatClient;
    private final ChatClient scottishToolChatClient;
    private final SystemMsgService systemMsgService;
    private final WebSearchMemoryService webSearchMemoryService;

    public OllamaService(
            @Qualifier("scottishChatClient") ChatClient scottishChatClient,
            @Qualifier("scottishToolChatClient") ChatClient scottishToolChatClient,
            SystemMsgService systemMsgService, WebSearchMemoryService webSearchMemoryService
    ) {
        this.scottishChatClient = scottishChatClient;
        this.scottishToolChatClient = scottishToolChatClient;
        this.systemMsgService = systemMsgService;
        this.webSearchMemoryService = webSearchMemoryService;
    }

    public String generateScottishResponse(String userMessage, String userId, String guildId, String channelId) {

        String conversationId = buildConversationId(userId, guildId, channelId);

        boolean useTools = shouldUseTools(userMessage);
        ChatClient client = useTools ? scottishToolChatClient : scottishChatClient;

        List<Message> messages = useTools
                ? List.of(new UserMessage(userMessage))
                : systemMsgService.systemMsg(userId, userMessage);
        try {
            String txt = extractTextResponse(
                    client.prompt()
                            .advisors(a -> {
                                if (!useTools) {
                                    a.param(ChatMemory.CONVERSATION_ID, conversationId);
                                }
                            })
                            .messages(messages)
                            .call()
                            .chatResponse()
            );
            txt = (txt == null) ? "" : txt.trim();

            if (txt.isBlank()) {
                throw new BlankModelResponseException("Model returned blank output");
            }

            log.debug("Ollama response (conversationId={}, useTools={}): {}", conversationId, useTools, txt);
            return txt;

        } catch (Exception e) {
            log.error("Ollama error (conversationId={}, useTools={}): {}", conversationId, useTools, e.getMessage());
        }

        return "I had a problem generating a response. Please try again.";
    }

    private String extractTextResponse(ChatResponse chatResponse) {
        try {
            if (chatResponse == null || chatResponse.getResult().getOutput().getText() == null) {
                return "";
            }
            return chatResponse.getResult().getOutput().getText();
        } catch (Exception e) {
            return "";
        }
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
