package com.discord.LocalAIDiscordAgent.promptBuilderChains.llmCallChains;

import com.discord.LocalAIDiscordAgent.comfyui.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiRunService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.service.FilterLLMService;
import com.discord.LocalAIDiscordAgent.llmMemory.records.ChatMemoryPayload;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llmAdvisors.booleanLLM.service.BooleanLLMService;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.toolCalls.LLMToolCalls;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RetrievedContext;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RuntimeContext;
import com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.service.StructuredLLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class LLMCallChain {

    private final PromptData promptData;
    private final DiscGlobalData discGlobalData;
    private final LLMToolCalls LLMToolCalls;
    private final BooleanLLMService booleanLLM;
    private final StructuredLLMService structuredLLM;
    private final FilterLLMService filterLLM;
    private final ComfyuiRunService comfyuiRunService;

    public LLMCallChain(
            DiscGlobalData discGlobalData,
            LLMToolCalls LLMToolCalls,
            PromptData promptData,
            BooleanLLMService booleanLLMService,
            StructuredLLMService structuredLLMService,
            FilterLLMService filterLLMService,
            ComfyuiRunService comfyuiRunService
    ) {
        this.discGlobalData = discGlobalData;
        this.promptData = promptData;
        this.LLMToolCalls = LLMToolCalls;
        this.booleanLLM = booleanLLMService;
        this.structuredLLM = structuredLLMService;
        this.filterLLM = filterLLMService;
        this.comfyuiRunService = comfyuiRunService;
    }

    public RuntimeContext executeContextChainRuntime() {
        boolean imageGenerationWasUsed = executeImageChain();

        if (imageGenerationWasUsed) {
            return null;
        }

        ChatMemoryPayload chatMemoryContext = executeChatMemoryChain();

        boolean useVectorMemory = executeUseVectorMemoryChain();

        if (!useVectorMemory) {
            executeWebSearchyChain();
        }

        return new RuntimeContext(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                discGlobalData.getUserProfile(),
                null,
                LLMToolCalls.callSummaryTool(),
                chatMemoryContext.longTermMemory(),
                chatMemoryContext.recentMessages(),
                chatMemoryContext.groupMemory(),
                null
        );
    }

    private ChatMemoryPayload executeChatMemoryChain() {
        boolean useChatMemory =  booleanLLM.useChatMemoryIfRelevant();
        return filterLLM.chatMemoryReducerFilter(useChatMemory);
    }

    private boolean executeUseVectorMemoryChain() {
        String query = structuredLLM.generateQuery();
        return booleanLLM.useVectorMemoryIfRelevant(query);
    }

    private boolean executeImageContextChain() {
        String query = structuredLLM.generateImageQuery();
        return booleanLLM.useImageContextIfValid(query);
    }

    private void executeWebSearchyChain() {
       boolean useWebSearch = booleanLLM.useWebSearchIfRequired();
       if (useWebSearch) {
           LLMToolCalls.callWebSearchTool();
       }
    }

    private boolean executeImageChain() {
        boolean useImageGeneration = booleanLLM.useImageGenerationIfRequested();
        if (!useImageGeneration) {
            return false;
        }

        if (executeImageContextChain()) {
            structuredLLM.summarizeImageContext();
        }

        try {
            ImageSettingsRecord settings = structuredLLM.generateImageSettings();
            log.info("Image Prompt: {}", settings);
            Path path = comfyuiRunService.generateImage(settings);
            discGlobalData.setImagePath(path);
            return true;
        } catch (Exception e) {
            log.error("Error generating image", e);
            return false;
        }
    }

}
