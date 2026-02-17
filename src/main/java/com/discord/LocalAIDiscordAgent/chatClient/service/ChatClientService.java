package com.discord.LocalAIDiscordAgent.chatClient.service;

import com.discord.LocalAIDiscordAgent.chatClient.helpers.ChatClientHelpers;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessChatClient;
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

import static com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey.USERNAME;

@Slf4j
@Service
public class ChatClientService {

    private final ChatClient chatClient;
    private final ProcessChatClient process;

    public ChatClientService(ChatClient advisorChatClient, ProcessChatClient processChatClient) {
        this.chatClient = advisorChatClient;
        this.process = processChatClient;
    }

    public String generateScottishResponse(String userMessage, Map<DiscDataKey, String> discDataMap) {
        String conversationId = ChatClientHelpers.buildConversationId(discDataMap);
        try {


            ChatResponse chatResponse = callLLM(userMessage, discDataMap);
            String assistantMessage = ChatClientHelpers.extractOutputTextAsString(chatResponse);

            log.debug("Ollama response (conversationId={}, extractedResponse={}): ", conversationId, assistantMessage);

            List<Message> messages = List.of(new UserMessage(userMessage), new AssistantMessage(assistantMessage));

            process.saveInteraction(conversationId, discDataMap.get(USERNAME), messages);

            return assistantMessage;
        } catch (Exception e) {
            log.error("Ollama error (conversationId={}): {}",
                    conversationId,
                    e.getMessage(),
                    e
            );
            return "I had a problem generating a response. Please try again.";
        }
    }

    private ChatResponse callLLM(String userMessage, Map<DiscDataKey, String> discDataMap) {
        return chatClient.prompt()
                .advisors(a ->
                        a.param(ChatMemory.CONVERSATION_ID, discDataMap.get(USERNAME))
                )
                .user(userMessage)
                .call()
                .chatResponse();
    }

}