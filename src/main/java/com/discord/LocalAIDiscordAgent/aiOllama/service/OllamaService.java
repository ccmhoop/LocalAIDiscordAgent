package com.discord.LocalAIDiscordAgent.aiOllama.service;

import com.discord.LocalAIDiscordAgent.AiContext.service.SystemMsgService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OllamaService {

    private static final long RESPONSE_TIMEOUT_SECONDS = 120L;

    private final ChatClient scottishChatClient;
    private final SystemMsgService systemMsgService;

    public OllamaService(ChatClient chatClientOllamaScottish, SystemMsgService systemMsgService) {
        this.scottishChatClient = chatClientOllamaScottish;
        this.systemMsgService = systemMsgService;
    }

    /**
     * Key objective:
     * - Prevent memory bleed across guilds/channels by using a composite conversation id.
     * - Keep tool execution separate from chat memory by relying on Spring AI's memory advisor behavior
     *   (tool internals should not be persisted into ChatMemory by default).
     */
    public String generateScottishResponse(String userMessage, String userId, String guildId, String channelId) {

        // IMPORTANT: isolate memory per guild + channel + user (recommended for Discord bots).
        String conversationId = buildConversationId(userId, guildId, channelId);

        List<Message> messages = systemMsgService.systemMsg(userId, userMessage);

        log.info("Preparing to connect to Ollama server with conversation ID: {}", conversationId);
        log.debug("Messages to send to Ollama: {}", messages);

        try {
            CompletableFuture<ChatResponse> futureResponse = CompletableFuture.supplyAsync(() -> {
                try {
                    return scottishChatClient
                            .prompt()
                            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                            .messages(messages)
                            .call()
                            .chatResponse();
                } catch (Exception e) {
                    log.error("Error in AI response generation: {}", e.getMessage(), e);
                    throw new RuntimeException("Error generating AI response", e);
                }
            });

            ChatResponse response = futureResponse.get(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response == null || response.getResult().getOutput().getText() == null || response.getResult().getOutput().getText().isBlank()) {
                log.warn("Ollama response was null/blank");
                return "I'm sorry, I didn't understand you.";
            }

            String responseText = response.getResult().getOutput().getText();
            log.info("Ollama response received successfully");
            log.debug("Ollama response: {}", responseText);
            return responseText;

        } catch (ResourceAccessException e) {
            log.error("Failed to connect to Ollama server. Details: {}", e.getMessage(), e);
            log.error("Connection attempted to: http://localhost:11434");
            return "I'm sorry, I'm having trouble connecting to my brain right now. The server appears to be running but isn't responding correctly. Please check the server status and logs.";

        } catch (TimeoutException e) {
            log.error("AI response timed out after {} seconds. This may be due to complex tool usage or advisor processing.", RESPONSE_TIMEOUT_SECONDS);
            return "I'm sorry, it's taking me too long to process your request. This might be happening because I'm trying to use tools or search for information. Please try a simpler question or try again later.";

        } catch (ExecutionException e) {
            log.error("Error in AI response execution. Type: {}, Message: {}", e.getCause().getClass().getName(), e.getCause().getMessage(), e);
            return "I'm sorry, something went wrong while processing your request. Please check the server logs for more details.";

        } catch (Exception e) {
            log.error("Error generating response. Type: {}, Message: {}", e.getClass().getName(), e.getMessage(), e);
            return "I'm sorry, something went wrong while processing your request. Please check the server logs for more details.";
        }
    }

    private String buildConversationId(String userId, String guildId, String channelId) {
        String safeGuild = (guildId == null || guildId.isBlank()) ? "dm" : guildId;
        String safeChannel = (channelId == null || channelId.isBlank()) ? "dm" : channelId;
        String safeUser = (userId == null || userId.isBlank()) ? "unknown-user" : userId;

        return safeGuild + ":" + safeChannel + ":" + safeUser;
    }
}
