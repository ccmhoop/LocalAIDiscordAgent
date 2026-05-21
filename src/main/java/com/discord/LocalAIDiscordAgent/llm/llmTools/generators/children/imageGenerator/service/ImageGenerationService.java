package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.service;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService.GeneratedFile;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.service.WebSearchService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.llmInstructions.ImageLLMInstructions;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.dto.ImageSettingsDTO;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.fileGeneration.ImageFileGeneration;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.preparation.ImageGenerationPreparation;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.service.FileGenerationChainService;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ImageGenerationService extends FileGenerationChainService<ImageSettingsDTO> {

    private final ImageGenerationPreparation preparation;
    private final ImageFileGeneration fileGeneration;
    private final ImageLLMInstructions llmInstructions;

    public ImageGenerationService(
            MapperUtils mapperUtils,
            ImageGenerationPreparation preparation,
            WebSearchService WebSearchService,
            ImageFileGeneration fileGeneration, ImageLLMInstructions llmInstructions
    ) {
        super(mapperUtils, WebSearchService);
        this.preparation = preparation;
        this.fileGeneration = fileGeneration;
        this.llmInstructions = llmInstructions;
    }

    @Override
    public Mono<GeneratedFile> executeLLMChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        return executeChain(discGlobalData, requiresContext, "image");
    }

    @Override
    protected ImageSettingsDTO llmInstructionsGenerateSetting(String userMessage, String context) {
        return llmInstructions.generateSettings(userMessage, context);
    }

    @Override
    protected Mono<GeneratedFile> generateFile(PromptData promptData) {
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
