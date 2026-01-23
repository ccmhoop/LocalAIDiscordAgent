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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OllamaService {

    private final ChatClient scottishChatClient;
    private final SystemMsgService systemMsgService;

    public OllamaService(ChatClient chatClientOllamaScottish, SystemMsgService systemMsgService) {
        this.scottishChatClient = chatClientOllamaScottish;
        this.systemMsgService = systemMsgService;
    }

    public String generateScottishResponse(String userMessage, String userId, String guildId, String channelId) {
        String conversationId = userId;
        List<Message> messages = systemMsgService.systemMsg(userId, userMessage);
        log.info("Preparing to connect to Ollama server with conversation ID: {}", conversationId);
        log.debug("Messages to send to Ollama: {}", messages);

        // Retry parameters
        int maxRetries = 3;
        long initialBackoffMillis = 1000; // 1 second

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    long backoffMillis = initialBackoffMillis * (long) Math.pow(2, attempt - 1);
                    log.info("Retry attempt {} after {} ms", attempt, backoffMillis);
                    TimeUnit.MILLISECONDS.sleep(backoffMillis);
                }

                log.info("Attempting to connect to Ollama server (attempt {}/{})", attempt + 1, maxRetries + 1);

                ChatResponse response = scottishChatClient
                        .prompt()
                        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .messages(messages)
                        .call()
                        .chatResponse();

                assert response != null;
                if (response.getResult().getOutput().getText() == null || response.getResult().getOutput().getText().isBlank()) {
                    log.warn("Ollama response was null or blank");
                    if (attempt == maxRetries) {
                        return "I'm sorry, I didn't understand you.";
                    }
                    continue;
                }

                String responseText = response.getResult().getOutput().getText();
                log.info("Ollama response received successfully");
                log.debug("Ollama response: {}", responseText);
                return responseText;

            } catch (ResourceAccessException e) {
                log.warn("Connection issue with Ollama server (attempt {}/{}). Details: {}", 
                        attempt + 1, maxRetries + 1, e.getMessage());

                if (attempt == maxRetries) {
                    log.error("Failed to connect to Ollama server after {} attempts. Details: {}", 
                            maxRetries + 1, e.getMessage(), e);
                    log.error("Connection attempted to: http://localhost:11434");
                    return "I'm sorry, I'm having trouble connecting to my brain right now. The server appears to be running but isn't responding correctly. Please check the server status and logs.";
                }
                // Continue to next retry attempt
            } catch (InterruptedException e) {
                log.error("Retry sleep interrupted", e);
                Thread.currentThread().interrupt();
                return "I'm sorry, something went wrong while processing your request. Please try again.";
            } catch (Exception e) {
                log.error("Error generating response (attempt {}/{}). Type: {}, Message: {}", 
                        attempt + 1, maxRetries + 1, e.getClass().getName(), e.getMessage(), e);

                if (attempt == maxRetries) {
                    return "I'm sorry, something went wrong while processing your request. Please check the server logs for more details.";
                }
                // Continue to next retry attempt
            }
        }

        // This should not be reached due to the returns in the catch blocks on the last attempt
        return "I'm sorry, I couldn't process your request after multiple attempts.";
    }
}
