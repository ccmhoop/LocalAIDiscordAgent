package com.discord.LocalAIDiscordAgent.chatClient.service;

import com.discord.LocalAIDiscordAgent.chatSummary.repository.ChatSummaryRepository;
import com.discord.LocalAIDiscordAgent.chatClient.helpers.ChatClientHelpers;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessChatClient;
import com.discord.LocalAIDiscordAgent.systemMessage.service.PromptService;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey.*;

@Slf4j
@Service
public class ChatClientService {

    private final ChatClient chatClient;
    private final ProcessChatClient process;
    private final PromptService promptService;


    public ChatClientService(ChatClient advisorChatClient, ProcessChatClient processChatClient, ChatSummaryRepository repo, PromptService promptService) {
        this.chatClient = advisorChatClient;
        this.process = processChatClient;
        this.promptService = promptService;
    }

    public String generateLLMResponse(Map<DiscDataKey, String> discDataMap, UserEntity userEntity) {
        try {
            ChatResponse chatResponse = callLLM(discDataMap);
            String assistantMessage = ChatClientHelpers.extractOutputTextAsString(chatResponse);
            log.debug("Ollama response (extractedResponse={}): ", assistantMessage);

            try {
                List<Message> messages = List.of(new UserMessage(discDataMap.get(USER_MESSAGE)), new AssistantMessage(assistantMessage));
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

    private ChatResponse callLLM(Map<DiscDataKey, String> discDataMap) {
        String systemPrompt = promptService.buildSystemMsgJson(discDataMap);
        log.info("Ollama prompt: {}", systemPrompt);
        Prompt prompt = Prompt.builder()
                .content(systemPrompt)
                .build();

        return chatClient.prompt(prompt)
                .call()
                .chatResponse();
    }

}
