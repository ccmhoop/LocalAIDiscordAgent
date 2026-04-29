package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.service;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.toolCalls.LLMToolCalls;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.llmGenerate.ImageSettingsLLMGenerate;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.dto.ImageSettingsDTO;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.fileGeneration.ImageFileGeneration;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.preparation.ImageGenerationPreparation;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.service.FileGenerationChain;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ImageGenerationChainService extends FileGenerationChain<ImageSettingsDTO> {

    private final ImageGenerationPreparation preparation;
    private final ImageFileGeneration fileGeneration;
    private final ImageSettingsLLMGenerate llmGenerate;

    public ImageGenerationChainService(
            MapperUtils mapperUtils,
            ImageGenerationPreparation preparation,
            LLMToolCalls LLMToolCalls,
            ImageFileGeneration fileGeneration, ImageSettingsLLMGenerate llmGenerate
    ) {
        super(mapperUtils, LLMToolCalls);
        this.preparation = preparation;
        this.fileGeneration = fileGeneration;
        this.llmGenerate = llmGenerate;
    }

    @Override
    public Mono<ComfyuiService.GeneratedFile> executeLLMChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        return executeChain(discGlobalData, requiresContext, "image");
    }

    @Override
    protected ImageSettingsDTO llmGenerateFileSettings(String userMessage, String context) {
        return llmGenerate.generateFileSettings(userMessage, context);
    }

    @Override
    protected Mono<ComfyuiService.GeneratedFile> generateFile(PromptData promptData) {
        return fileGeneration.generateImageFile(promptData);
    }

    @Override
    protected String prepareUserMessage(DiscGlobalData discGlobalData) {
        return preparation.prepareUserMessage(discGlobalData);
    }

    @Override
    protected void prepareSettingsDTO(PromptData promptData, ImageSettingsDTO settingsPayload) {
        preparation.prepareSettingsDTO(promptData, settingsPayload);
    }

    @Override
    protected void afterPrepare(PromptData promptData) {
        log.info("Image Prompt: {}", promptData.getImageSettingsDTO());
    }
}
