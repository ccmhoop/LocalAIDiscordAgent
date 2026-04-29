package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.service;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.toolCalls.LLMToolCalls;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RequiredArgsConstructor
public abstract class FileGenerationChain<T extends Record> {

    protected final MapperUtils mapperUtils;
    private final LLMToolCalls LLMToolCalls;

    protected Mono<ComfyuiService.GeneratedFile> executeChain(
            DiscGlobalData discGlobalData,
            boolean requiresContext,
            String generationType
    ) {
        return Mono.fromCallable(() -> {
                    PromptData promptData = new PromptData(mapperUtils);
                    String userMessage = prepareUserMessage(discGlobalData);

                    T settings = llmGenerateFileSettings(userMessage, "");
                    prepareSettingsDTO(promptData, settings);

                    afterPrepare(promptData);

                    return promptData;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::generateFile)
                .doOnError(error -> log.error("Error generating {}", generationType, error));
    }
    protected abstract Mono<ComfyuiService.GeneratedFile> executeLLMChain(DiscGlobalData discGlobalData, boolean requiresContext);
    protected abstract T llmGenerateFileSettings(String userMessage, String context);
    protected abstract Mono<ComfyuiService.GeneratedFile> generateFile(PromptData promptData);
    protected abstract String prepareUserMessage(DiscGlobalData discGlobalData);
    protected abstract void prepareSettingsDTO(PromptData promptData, T settingsPayload);
    protected abstract void afterPrepare(PromptData promptData);

}