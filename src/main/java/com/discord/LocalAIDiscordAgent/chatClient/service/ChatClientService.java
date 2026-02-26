package com.discord.LocalAIDiscordAgent.chatClient.service;

import com.discord.LocalAIDiscordAgent.chatClient.helpers.ChatClientHelpers;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessChatClient;
import com.discord.LocalAIDiscordAgent.user.UserEntity;
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

import static com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey.*;

@Slf4j
@Service
public class ChatClientService {

    private final ChatClient chatClient;
    private final ProcessChatClient process;

    public ChatClientService(ChatClient advisorChatClient, ProcessChatClient processChatClient) {
        this.chatClient = advisorChatClient;
        this.process = processChatClient;
    }

    public String generateLLMResponse(String userMessage, Map<DiscDataKey, String> discDataMap, UserEntity userEntity) {
        try {
            ChatResponse chatResponse = callLLM(userMessage, discDataMap);
            String assistantMessage = ChatClientHelpers.extractOutputTextAsString(chatResponse);
            log.debug("Ollama response (extractedResponse={}): ",  assistantMessage);

            try {
                List<Message> messages = List.of(new UserMessage(userMessage), new AssistantMessage(assistantMessage));
                process.saveInteraction(discDataMap, messages, userEntity);
                log.debug("Successfully saved chat interaction for user: {}", discDataMap.get(USER_ID));
            } catch (Exception saveException) {
                log.error("Failed to save chat memory for user: {} - Error: {}",
                    discDataMap.get(USER_ID), saveException.getMessage(), saveException);
            }
            return assistantMessage;
        } catch (Exception e) {
            log.error("Ollama error ({}",
                    e.getMessage(),
                    e
            );
            return "I had a problem generating a response. Please try again.";
        }
    }

    private ChatResponse callLLM(String userMessage, Map<DiscDataKey, String> discDataMap) {

        Map<String, Object> advisorParams = Map.of(
                "chat_memory_conversation_id", discDataMap.get(USER_ID),
                "guild_id", discDataMap.get(GUILD_ID)
        );


        return chatClient.prompt()
                .advisors(a ->
                        a.params(advisorParams)
                )
                .user(userMessage)
                .call()
                .chatResponse();
    }

}
