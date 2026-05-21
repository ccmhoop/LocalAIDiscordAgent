package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.service;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService.GeneratedFile;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.service.WebSearchService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.llmInstructions.VideoLLMInstructions;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.dto.VideoSettingsDTO;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.fileGeneration.VideoFileGeneration;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.preparation.VideoGenerationPreparation;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.service.FileGenerationChainService;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class VideoGenerationService extends FileGenerationChainService<VideoSettingsDTO> {

    private final VideoGenerationPreparation preparation;
    private final VideoFileGeneration fileGeneration;
    private final VideoLLMInstructions llmInstructions;

    public VideoGenerationService(
            MapperUtils mapperUtils,
            VideoGenerationPreparation preparation,
            WebSearchService WebSearchService, VideoFileGeneration fileGeneration, VideoLLMInstructions llmInstructions
    ) {
        super(mapperUtils, WebSearchService);
        this.preparation = preparation;
        this.fileGeneration = fileGeneration;
        this.llmInstructions = llmInstructions;
    }

    @Override
    public Mono<GeneratedFile> executeLLMChain(
            DiscGlobalData discGlobalData,
            boolean requiresContext
    ) {
        return executeChain(discGlobalData, requiresContext, "video");
    }

    @Override
    protected VideoSettingsDTO llmInstructionsGenerateSetting(String userMessage, String context) {
        return llmInstructions.generateSettings(userMessage, context);
    }

    @Override
    protected Mono<GeneratedFile> generateFile(PromptData promptData) {
        return fileGeneration.generateVideoFile(promptData);
    }

    @Override
    protected String prepareUserMessage(DiscGlobalData discGlobalData) {
        return preparation.prepareUserMessage(discGlobalData);
    }

    @Override
    protected void prepareSettingsDTO(PromptData promptData, VideoSettingsDTO settingsPayload) {
        preparation.prepareSettingsDTO( promptData, settingsPayload);
    }

    @Override
    protected void afterPrepare(PromptData promptData) {
        log.info("Video Prompt: {}", promptData.getVideoSettings());
    }
}