package com.discord.LocalAIDiscordAgent.promptBuilderChains.llmCallChains;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiRunService;
import com.discord.LocalAIDiscordAgent.comfyui.imageAdvisor.ImageSettingsPreparationService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmRouteDecider.RouteDecisionPreparationService;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.chatMemoryAdvisor.ChatMemoryPreparationService;
import com.discord.LocalAIDiscordAgent.ragMemory.ragAdvisor.RagContextPreparationService;
import com.discord.LocalAIDiscordAgent.llmRouteDecider.records.RouteDecision;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.toolCalls.LLMToolCalls;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RetrievedContext;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RuntimeContext;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchPreparationService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class LLMCallChain {

    private final LLMToolCalls LLMToolCalls;
    private final DiscGlobalData discGlobalData;
    private final RouteDecisionPreparationService routeDecisionService;
    private final ComfyuiRunService comfyuiRunService;
    private final ChatMemoryPreparationService chatMemoryService;
    private final RagContextPreparationService ragContextService;
    private final MapperUtils mapperUtils;
    private final WebSearchPreparationService webSearchPreparationService;
    private final ImageSettingsPreparationService imageSettingsPreparationService;


    public LLMCallChain(
            ComfyuiRunService comfyuiRunService,
            DiscGlobalData discGlobalData,
            LLMToolCalls LLMToolCalls,
            RouteDecisionPreparationService RouteDecisionPreparationService,
            ChatMemoryPreparationService chatMemoryPreparationService,
            RagContextPreparationService ragContextService,
            MapperUtils mapperUtils,
            WebSearchPreparationService webSearchPreparationService,
            ImageSettingsPreparationService imageSettingsPreparationService
    ) {
        this.LLMToolCalls = LLMToolCalls;
        this.discGlobalData = discGlobalData;
        this.comfyuiRunService = comfyuiRunService;
        this.routeDecisionService = RouteDecisionPreparationService;
        this.chatMemoryService = chatMemoryPreparationService;
        this.ragContextService = ragContextService;
        this.mapperUtils = mapperUtils;
        this.webSearchPreparationService = webSearchPreparationService;
        this.imageSettingsPreparationService = imageSettingsPreparationService;
    }

    public RuntimeContext executeContextChainRuntime() {
        PromptData promptData = new PromptData(mapperUtils);
        RouteDecision decision = routeDecisionService.prepare(discGlobalData);
        switch (decision.mode()) {
            case IMAGE -> {
                executeImageChain(promptData);
                return null;
            }
            case TEXT -> {
                return executeTextResponseChain(promptData);
            }
        }

        return null;
    }

    @NotNull
    private RuntimeContext executeTextResponseChain(PromptData promptData) {

        chatMemoryService.prepare(discGlobalData, promptData);
        ragContextService.prepare(discGlobalData, promptData);

        if (promptData.getRetrievedContext() == null) {
            webSearchPreparationService.prepare(discGlobalData, promptData);
            executeWebSearchyChain(promptData);
        }

        if (promptData.getRetrievedContext() != null) {
            LLMToolCalls.callSummaryTool(promptData);
        }

        return new RuntimeContext(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                discGlobalData.getUserProfile(),
                null,
                promptData.getSummary() == null? null : new RetrievedContext(promptData.getSummary()),
                promptData.getChatMemoryPayload().longTermMemory(),
                promptData.getChatMemoryPayload().recentMessages(),
                promptData.getChatMemoryPayload().groupMemory(),
                null
        );
    }

    private void executeWebSearchyChain(PromptData promptData) {
        if (promptData.isWebSearchRequired()) {
            LLMToolCalls.callWebSearchTool();
        }
    }

    private void executeImageChain(PromptData promptData) {
        ragContextService.prepare(discGlobalData, promptData);

        if (promptData.getRetrievedContext() != null) {
            LLMToolCalls.callSummaryTool(promptData);
        }

        try {
            imageSettingsPreparationService.prepare(discGlobalData, promptData);
            log.info("Image Prompt: {}", promptData.getImageSettings());
            Path path = comfyuiRunService.generateImage(promptData);
            discGlobalData.setImagePath(path);
        } catch (Exception e) {
            log.error("Error generating image", e);
        }
        promptData.setSummary(null);
    }

}
