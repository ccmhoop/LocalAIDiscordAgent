package com.discord.LocalAIDiscordAgent.llm.llmChains.llmCallChains;

import com.discord.LocalAIDiscordAgent.memory.chatMemory.chatMemoryAdvisor.ChatMemoryPreparationService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.service.ImagePrepareSettingsPayloadService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.service.MusicPrepareSettingsPayloadService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.service.VideoPrepareSettingsPayloadService;
import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiRunService;
import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmRouteDecider.RouteDecisionPreparationService;
import com.discord.LocalAIDiscordAgent.llm.llmRouteDecider.records.RouteDecision;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.toolCalls.LLMToolCalls;
import com.discord.LocalAIDiscordAgent.memory.ragMemory.ragAdvisor.RagContextPreparationService;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.RetrievedContext;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.RuntimeContext;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.service.WebSearchPreparationService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class LLMCallChain {

    private final LLMToolCalls LLMToolCalls;
    private final RouteDecisionPreparationService routeDecisionService;
    private final ComfyuiRunService comfyuiRunService;
    private final ChatMemoryPreparationService chatMemoryService;
    private final RagContextPreparationService ragContextService;
    private final MapperUtils mapperUtils;
    private final WebSearchPreparationService webSearchPreparationService;
    private final ImagePrepareSettingsPayloadService imagePreparePayloadService;
    private final MusicPrepareSettingsPayloadService musicGenerationService;
    private final VideoPrepareSettingsPayloadService videoService;

    public LLMCallChain(
            ComfyuiRunService comfyuiRunService,
            LLMToolCalls LLMToolCalls,
            RouteDecisionPreparationService routeDecisionPreparationService,
            ChatMemoryPreparationService chatMemoryPreparationService,
            RagContextPreparationService ragContextService,
            MapperUtils mapperUtils,
            WebSearchPreparationService webSearchPreparationService,
            ImagePrepareSettingsPayloadService imagePreparePayloadService,
            MusicPrepareSettingsPayloadService musicGenerationService,
            VideoPrepareSettingsPayloadService videoService
    ) {
        this.LLMToolCalls = LLMToolCalls;
        this.comfyuiRunService = comfyuiRunService;
        this.routeDecisionService = routeDecisionPreparationService;
        this.chatMemoryService = chatMemoryPreparationService;
        this.ragContextService = ragContextService;
        this.mapperUtils = mapperUtils;
        this.webSearchPreparationService = webSearchPreparationService;
        this.imagePreparePayloadService = imagePreparePayloadService;
        this.musicGenerationService = musicGenerationService;
        this.videoService = videoService;
    }

    public RouteDecision decideRoute(DiscGlobalData discGlobalData) {
        return routeDecisionService.prepare(discGlobalData);
    }

    public RuntimeContext executeTextContextRuntime(DiscGlobalData discGlobalData, boolean requiresContext) {
        PromptData promptData = new PromptData(mapperUtils);
        return executeTextResponseChain(discGlobalData, promptData, requiresContext);
    }

    public Mono<ComfyuiService.GeneratedFile> executeImageChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        PromptData promptData = new PromptData(mapperUtils);

        return Mono.fromCallable(() -> {
                    ragContextService.prepare(discGlobalData, promptData, requiresContext);

                    if (promptData.getRetrievedContext() != null) {
                        LLMToolCalls.callSummaryTool(promptData, discGlobalData);
                    }

                    imagePreparePayloadService.prepare(discGlobalData, promptData);
                    log.info("Image Prompt: {}", promptData.getImageSettings());
                    return promptData;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(comfyuiRunService::generateImage)
                .doOnError(error -> log.error("Error generating image", error));
    }

    public Mono<ComfyuiService.GeneratedFile> executeVideoChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        PromptData promptData = new PromptData(mapperUtils);

        return Mono.fromCallable(() -> {
                    ragContextService.prepare(discGlobalData, promptData, requiresContext);

                    if (promptData.getRetrievedContext() != null) {
                        LLMToolCalls.callSummaryTool(promptData, discGlobalData);
                    }

                    videoService.prepare(discGlobalData, promptData);
                    return promptData;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(comfyuiRunService::generateVideo)
                .doOnError(error -> log.error("Error generating video", error));
    }

    public Mono<ComfyuiService.GeneratedFile> executeMusicChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        PromptData promptData = new PromptData(mapperUtils);

        return Mono.fromCallable(() -> {
                    ragContextService.prepare(discGlobalData, promptData, requiresContext);

                    if (promptData.getRetrievedContext() != null) {
                        LLMToolCalls.callSummaryTool(promptData, discGlobalData);
                    }

                    musicGenerationService.prepare(discGlobalData, promptData);
                    return promptData;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(comfyuiRunService::generateMusic)
                .doOnError(error -> log.error("Error generating music", error));
    }

    @NotNull
    private RuntimeContext executeTextResponseChain(DiscGlobalData discGlobalData, PromptData promptData, boolean requiresContext) {
        chatMemoryService.prepare(discGlobalData, promptData);
        ragContextService.prepare(discGlobalData, promptData, requiresContext);

        if (promptData.getRetrievedContext() == null) {
            webSearchPreparationService.prepare(discGlobalData, promptData);
            executeWebSearchyChain( discGlobalData, promptData);
        }

        if (promptData.getRetrievedContext() != null) {
            LLMToolCalls.callSummaryTool(promptData, discGlobalData);
        }

        return new RuntimeContext(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                discGlobalData.getUserProfile(),
                null,
                promptData.getSummary() == null ? null : new RetrievedContext(promptData.getSummary()),
                promptData.getChatMemoryPayload().longTermMemory(),
                promptData.getChatMemoryPayload().recentMessages(),
                promptData.getChatMemoryPayload().groupMemory(),
                null
        );
    }

    private void executeWebSearchyChain(DiscGlobalData discGlobalData, PromptData promptData) {
        if (promptData.isWebSearchRequired()) {
            LLMToolCalls.callWebSearchTool(discGlobalData);
        }
    }
}