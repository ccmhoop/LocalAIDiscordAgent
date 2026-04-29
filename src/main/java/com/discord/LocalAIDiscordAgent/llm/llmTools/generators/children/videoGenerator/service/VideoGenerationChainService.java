package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.service;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.toolCalls.LLMToolCalls;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.llmGenerate.VideoSettingsLLMGenerate;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.dto.VideoSettingsDTO;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.fileGeneration.VideoFileGeneration;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.preparation.VideoGenerationPreparation;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.service.FileGenerationChain;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class VideoGenerationChainService extends FileGenerationChain<VideoSettingsDTO> {

    private final VideoGenerationPreparation preparation;
    private final VideoFileGeneration fileGeneration;
    private final VideoSettingsLLMGenerate llmGenerate;

    public VideoGenerationChainService(
            MapperUtils mapperUtils,
            VideoGenerationPreparation preparation,
            LLMToolCalls LLMToolCalls, VideoFileGeneration fileGeneration, VideoSettingsLLMGenerate llmGenerate
    ) {
        super(mapperUtils, LLMToolCalls);
        this.preparation = preparation;
        this.fileGeneration = fileGeneration;
        this.llmGenerate = llmGenerate;
    }

    @Override
    public Mono<ComfyuiService.GeneratedFile> executeLLMChain(
            DiscGlobalData discGlobalData,
            boolean requiresContext
    ) {
        return executeChain(discGlobalData, requiresContext, "video");
    }

    @Override
    protected VideoSettingsDTO llmGenerateFileSettings(String userMessage, String context) {
        return llmGenerate.generateFileSettings(userMessage, context);
    }

    @Override
    protected Mono<ComfyuiService.GeneratedFile> generateFile(PromptData promptData) {
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