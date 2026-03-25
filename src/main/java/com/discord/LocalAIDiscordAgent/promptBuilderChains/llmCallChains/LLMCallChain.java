package com.discord.LocalAIDiscordAgent.promptBuilderChains.llmCallChains;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiRunService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.service.LLMClientQueryGeneratorService.ImagePromptOutput;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.generatorCalls.LLMGeneratorCalls;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.memoryCalls.LLMMemoryCalls;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.validationCalls.LLMValidationCalls;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.toolCalls.LLMToolCalls;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RetrievedContext;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RuntimeContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
public class LLMCallChain {

    private final PromptData promptData;
    private final DiscGlobalData discGlobalData;
    private final LLMToolCalls LLMToolCalls;
    private final LLMValidationCalls LLMValidationCalls;
    private final LLMGeneratorCalls LLMGeneratorCalls;
    private final LLMMemoryCalls llmMemoryCalls;
    private final ComfyuiRunService comfyuiRunService;


    public LLMCallChain(
            DiscGlobalData discGlobalData, LLMValidationCalls LLMValidationCalls,
            LLMToolCalls LLMToolCalls,
            PromptData promptData, LLMGeneratorCalls llmGeneratorCalls, LLMMemoryCalls llmMemoryCalls, ComfyuiRunService comfyuiRunService
    ) {
        this.discGlobalData = discGlobalData;
        this.promptData = promptData;
        this.LLMToolCalls = LLMToolCalls;
        this.LLMValidationCalls = LLMValidationCalls;
        LLMGeneratorCalls = llmGeneratorCalls;
        this.llmMemoryCalls = llmMemoryCalls;
        this.comfyuiRunService = comfyuiRunService;
    }

    public RuntimeContext executeContextChainRuntime() {
        boolean useImageGeneration = executeImageChain();
        log.info("Use Image Generation: {}", useImageGeneration);
        if (useImageGeneration) {
            return null;
        }
        String contextSummary = executeContextChain();
        RetrievedContext retrievedContext = new RetrievedContext(contextSummary);
        RuntimeContext chatMemoryContext = llmMemoryCalls.filterChatMemory();
        log.info("Chat Memory Context: {}", chatMemoryContext);
        return new RuntimeContext(
                null,
                null,
                null,
                retrievedContext,
                chatMemoryContext.longTermMemory(),
                chatMemoryContext.recentMessages(),
                chatMemoryContext.groupMemory(),
                null
        );
    }

    private String executeContextChain() {

        String query = LLMGeneratorCalls.chatBasedQuery();

        log.info("Query: {}", query);

        boolean useVectorDbMemory = LLMValidationCalls.isVectorDBMemoryValid(query);
        log.info("Use Vector DB Memory: {}", useVectorDbMemory);

        boolean useWebSearch = false;

        if (!useVectorDbMemory) {
            useWebSearch = LLMValidationCalls.isWebSearch();
        }
        log.info("Use Web Search Memory: {}", useWebSearch);

        if (!useVectorDbMemory && !useWebSearch) {
            return null;
        }


        if (useWebSearch) {
            LLMToolCalls.callWebSearchTool();
        }

         return LLMToolCalls.callSummaryTool();

    }

    private boolean executeImageChain() {
        if (!LLMValidationCalls.isImageGeneration()) {
            return false;
        }

        try {
            ImagePromptOutput outPut = LLMGeneratorCalls.imagePrompt();

            log.info("Image Prompt: {}", outPut);

            String positivePrompt = outPut.positivePrompt();
            String negativePrompt = outPut.negativePrompt();
            int width = outPut.pixelWidth();
            int height = outPut.pixelHeight();

            Path path = comfyuiRunService.generateImage(positivePrompt, negativePrompt, width, height);
            discGlobalData.setImagePath(path);
            return true;

        } catch (Exception e) {
            log.error("Error generating image", e);
            return false;
        }
    }



}
