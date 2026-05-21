package com.discord.LocalAIDiscordAgent.llm.llmChains.llmChain;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService.GeneratedFile;
import com.discord.LocalAIDiscordAgent.llm.llmRouter.service.LLMRouterService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.service.FileGenerationService;
import com.discord.LocalAIDiscordAgent.memory.chatMemory.chatMemoryAdvisor.ChatMemoryPreparationService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmRouter.dto.LLMRouterDTO;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.service.WebSearchService;
import com.discord.LocalAIDiscordAgent.memory.ragMemory.ragAdvisor.RagContextPreparationService;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.RetrievedContext;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.RuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class LLMCallChain {

    private final MapperUtils mapperUtils;
    private final WebSearchService webSearchService;
    private final LLMRouterService llmRouterService;
    private final ChatMemoryPreparationService chatMemoryService;
    private final RagContextPreparationService ragContextService;
    private final FileGenerationService fileGeneratorService;

    public LLMCallChain(
            MapperUtils mapperUtils,
            WebSearchService webSearchService,
            LLMRouterService llmRouterService,
            RagContextPreparationService ragContextService,
            FileGenerationService fileGeneratorService,
            ChatMemoryPreparationService chatMemoryPreparationService
    ) {
        this.chatMemoryService = chatMemoryPreparationService;
        this.fileGeneratorService = fileGeneratorService;
        this.ragContextService = ragContextService;
        this.llmRouterService = llmRouterService;
        this.webSearchService = webSearchService;
        this.mapperUtils = mapperUtils;
    }

    public LLMRouterDTO decideRoute(DiscGlobalData discGlobalData) {
        return llmRouterService.callLLMRouter(discGlobalData);
    }

    public RuntimeContext executeTextContextRuntime(DiscGlobalData discGlobalData, boolean requiresContext) {
        PromptData promptData = new PromptData(mapperUtils);
        return executeTextResponseChain(discGlobalData, promptData, requiresContext);
    }

    public Mono<GeneratedFile> executeImageChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        return fileGeneratorService.imageGenerationChain(discGlobalData, requiresContext);
    }

    public Mono<GeneratedFile> executeVideoChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        return fileGeneratorService.videoGenerationChain(discGlobalData, requiresContext);
    }

    public Mono<GeneratedFile> executeMusicChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        return fileGeneratorService.musicGenerationChain(discGlobalData, requiresContext);
    }

    @NotNull
    private RuntimeContext executeTextResponseChain(DiscGlobalData discGlobalData, PromptData promptData, boolean requiresContext) {
        chatMemoryService.prepare(discGlobalData, promptData);
        ragContextService.prepare(discGlobalData, promptData, requiresContext);

        if (promptData.getRetrievedContext() == null) {
            webSearchService.handleWebSearch(discGlobalData, promptData);
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

}