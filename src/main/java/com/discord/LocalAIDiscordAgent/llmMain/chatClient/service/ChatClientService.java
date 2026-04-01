package com.discord.LocalAIDiscordAgent.llmMain.chatClient.service;

import com.discord.LocalAIDiscordAgent.llmMain.chatClient.helpers.ChatClientHelpers;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessChatClient;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.PromptService;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class ChatClientService {

    private final ChatClient chatClient;
    private final ProcessChatClient process;
    private final PromptService promptService;
    private final DiscGlobalData discGlobalData;

    public ChatClientService(
            PromptService promptService,
            ChatClient advisorChatClient,
            DiscGlobalData discGlobalData,
            ProcessChatClient processChatClient
    ) {
        this.discGlobalData = discGlobalData;
        this.chatClient = advisorChatClient;
        this.promptService = promptService;
        this.process = processChatClient;
    }

    public String generateLLMResponse(UserEntity userEntity) {
        try {
            ChatResponse chatResponse = callLLM();
            if (chatResponse == null) {
                return  null;
            }
            String assistantMessage = ChatClientHelpers.extractOutputTextAsString(chatResponse);
            log.debug("Ollama response (extractedResponse={}): ", assistantMessage);

            try {
                process.saveInteraction(
                        new UserMessage(discGlobalData.getUserMessage()),
                        new AssistantMessage(assistantMessage),
                        userEntity
                );
                log.debug("Successfully saved chat interaction for user: {}", discGlobalData.getUserId());
            } catch (Exception saveException) {
                log.error("Failed to save chat memory for user: {} - Error: {}",
                        discGlobalData.getUserId(), saveException.getMessage(), saveException);
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

    private ChatResponse callLLM() {
        String systemPrompt = promptService.getSystemPromptAsJson();
        if (systemPrompt == null) {
            return null;
        }
        Prompt prompt = Prompt.builder()
                .messages(
                        List.of(
                                new SystemMessage(systemPrompt),
                                new UserMessage(discGlobalData.getUserMessage()
                                )
                        )
                )
                .build();
        log.info("Ollama prompt: {}", prompt);

        return chatClient.prompt(prompt)
                .call()
                .chatResponse();
    }

}
