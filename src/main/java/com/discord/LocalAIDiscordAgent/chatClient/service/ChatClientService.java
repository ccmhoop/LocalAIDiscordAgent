package com.discord.LocalAIDiscordAgent.chatClient.service;

import com.discord.LocalAIDiscordAgent.chatClient.helpers.ChatClientHelpers;
import com.discord.LocalAIDiscordAgent.chatMemory.service.RecentChatMemoryService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class ChatClientService {

    private final ChatClient chatClient;
    private final RecentChatMemoryService recentChatMemoryService;

    public ChatClientService(ChatClient advisorChatClient, RecentChatMemoryService recentChatMemoryService) {
        this.chatClient = advisorChatClient;
        this.recentChatMemoryService = recentChatMemoryService;
    }

    public String generateScottishResponse(String userMessage, Map<String, String> metadata) {

        String conversationId = ChatClientHelpers.buildMetaDataConversationId(metadata);

        try {
            ChatResponse chatResponse =
                    chatClient.prompt()
                            .advisors(a ->
                                    a.param(ChatMemory.CONVERSATION_ID, metadata.get("username"))
                            )
                            .user(userMessage)
                            .call()
                            .chatResponse();

            String extractedResponse = ChatClientHelpers.extractOutputTextAsString(chatResponse);

            log.debug("Ollama response (conversationId={}, extractedResponse={}): ", conversationId, extractedResponse);

            List<Message> messages =  List.of(new UserMessage(userMessage), new AssistantMessage(extractedResponse));

            recentChatMemoryService.processInteraction(conversationId, metadata.get("username"), messages);

            return extractedResponse;

        } catch (Exception e) {
            log.error("Ollama error (conversationId={}): {}",
                    conversationId,
                    e.getMessage(),
                    e
            );
            return "I had a problem generating a response. Please try again.";
        }
    }

}
