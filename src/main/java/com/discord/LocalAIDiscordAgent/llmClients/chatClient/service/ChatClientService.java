package com.discord.LocalAIDiscordAgent.llmClients.chatClient.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalDataContextHolder;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessChatClient;
import com.discord.LocalAIDiscordAgent.llmClients.chatClient.helpers.ChatClientHelpers;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.PromptService;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@Service
public class ChatClientService {

    private final ChatClient chatClient;
    private final ProcessChatClient process;
    private final PromptService promptService;

    public ChatClientService(
            PromptService promptService,
            ChatClient advisorChatClient,
            ProcessChatClient processChatClient
    ) {
        this.chatClient = advisorChatClient;
        this.promptService = promptService;
        this.process = processChatClient;
    }

    public Mono<String> generateLLMResponse(UserEntity userEntity, DiscGlobalData discGlobalData) {
        return Mono.fromCallable(() -> callLLM(discGlobalData))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(chatResponse -> {
                    if (chatResponse == null) {
                        return Mono.empty();
                    }

                    String assistantMessage = ChatClientHelpers.extractOutputTextAsString(chatResponse);
                    log.debug("Ollama response: {}", assistantMessage);

                    return process.saveInteraction(
                                    new UserMessage(discGlobalData.getUserMessage()),
                                    new AssistantMessage(assistantMessage),
                                    userEntity
                            )
                            .contextWrite(ctx -> DiscGlobalDataContextHolder.put(ctx, discGlobalData))
                            .thenReturn(assistantMessage);
                })
                .doOnSuccess(response -> {
                    if (response != null) {
                        log.debug("Successfully saved chat interaction for user: {}", discGlobalData.getUserId());
                    }
                })
                .onErrorResume(e -> {
                    log.error("Ollama error ({})", e.getMessage(), e);
                    return Mono.just("I had a problem generating a response. Please try again.");
                });
    }

    private ChatResponse callLLM(DiscGlobalData discGlobalData) {
        String systemPrompt = promptService.getSystemPromptAsJson(discGlobalData);
        if (systemPrompt == null) {
            return null;
        }

        Prompt prompt = Prompt.builder()
                .messages(List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(discGlobalData.getUserMessage())
                ))
                .build();

        log.info("Ollama prompt: {}", prompt);

        return chatClient.prompt(prompt)
                .call()
                .chatResponse();
    }
}