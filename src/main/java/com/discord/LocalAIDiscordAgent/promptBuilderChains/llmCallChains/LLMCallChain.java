package com.discord.LocalAIDiscordAgent.promptBuilderChains.llmCallChains;

import com.discord.LocalAIDiscordAgent.comfyui.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiRunService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.memoryCalls.LLMMemoryCalls;
import com.discord.LocalAIDiscordAgent.resolverLLM.service.ResolverLLMService;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.toolCalls.LLMToolCalls;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RetrievedContext;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RuntimeContext;
import com.discord.LocalAIDiscordAgent.structuredLLM.service.StructuredLLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
public class LLMCallChain {

    private final PromptData promptData;
    private final DiscGlobalData discGlobalData;
    private final LLMToolCalls LLMToolCalls;
    private final ResolverLLMService resolverLLM;
    private final StructuredLLMService textLLM;
    private final LLMMemoryCalls llmMemoryCalls;
    private final ComfyuiRunService comfyuiRunService;

    public LLMCallChain(
            DiscGlobalData discGlobalData,
            LLMToolCalls LLMToolCalls,
            PromptData promptData,
            ResolverLLMService resolverLLMService,
            StructuredLLMService structuredLLMService,
            LLMMemoryCalls llmMemoryCalls,
            ComfyuiRunService comfyuiRunService
    ) {
        this.discGlobalData = discGlobalData;
        this.promptData = promptData;
        this.LLMToolCalls = LLMToolCalls;
        this.resolverLLM = resolverLLMService;
        this.textLLM = structuredLLMService;
        this.llmMemoryCalls = llmMemoryCalls;
        this.comfyuiRunService = comfyuiRunService;
    }

    public RuntimeContext executeContextChainRuntime() {
        boolean useImageGeneration = executeImageChain();
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

        String query = textLLM.generateQuery();

        log.info("Query: {}", query);

        boolean useVectorDbMemory = resolverLLM.useVectorMemory(query);
        log.info("Use Vector DB Memory: {}", useVectorDbMemory);

        boolean useWebSearch = false;

        boolean useChatMemory =  resolverLLM.useChatMemory();
        log.info("Use Chat Memory: {}", useChatMemory);

        if (!useVectorDbMemory) {
            useWebSearch = resolverLLM.useWebSearch();
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
        boolean useImageGeneration = resolverLLM.useImageGeneration();
        log.info("Use Image Generation: {}", useImageGeneration);
        if (!useImageGeneration) {
            return false;
        }

        try {
            ImageSettingsRecord settings = textLLM.generateImageSettings();

            log.info("Image Prompt: {}", settings);

            String positivePrompt = settings.positivePrompt();
            String negativePrompt = settings.negativePrompt();
            int width = settings.pixelWidth();
            int height = settings.pixelHeight();

            Path path = comfyuiRunService.generateImage(positivePrompt, negativePrompt, width, height);
            discGlobalData.setImagePath(path);
            return true;

        } catch (Exception e) {
            log.error("Error generating image", e);
            return false;
        }
    }



}
