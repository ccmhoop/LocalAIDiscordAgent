package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.service;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService.GeneratedFile;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.service.WebSearchService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.llmInstructions.MusicLLMInstructions;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.dto.MusicSettingsDTO;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.fileGeneration.MusicFileGeneration;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.preparation.MusicGenerationPreparation;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.service.FileGenerationChainService;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class MusicGenerationService extends FileGenerationChainService<MusicSettingsDTO> {

    private final MusicGenerationPreparation preparation;
    private final MusicLLMInstructions llmInstructions;
    private final MusicFileGeneration fileGeneration;

    public MusicGenerationService(
            MapperUtils mapperUtils,
            MusicGenerationPreparation preparation,
            WebSearchService WebSearchService,
            MusicLLMInstructions llmGeneration,
            MusicFileGeneration fileGeneration
    ) {
        super(mapperUtils, WebSearchService);
        this.preparation = preparation;
        this.llmInstructions = llmGeneration;
        this.fileGeneration = fileGeneration;
    }

    @Override
    public Mono<GeneratedFile> executeLLMChain(
            DiscGlobalData discGlobalData,
            boolean requiresContext
    ) {
        return executeChain(discGlobalData, requiresContext, "music");
    }

    @Override
    protected MusicSettingsDTO llmInstructionsGenerateSetting(String userMessage, String context) {
        return llmInstructions.generateSettings(userMessage, context);
    }

    @Override
    protected Mono<GeneratedFile> generateFile(PromptData promptData) {
        return fileGeneration.generateMusicFile(promptData);
    }

    @Override
    protected String prepareUserMessage(DiscGlobalData discGlobalData) {
        return preparation.prepareUserMessage(discGlobalData);
    }

    @Override
    protected void prepareSettingsDTO(PromptData promptData, MusicSettingsDTO settingsPayload) {
        preparation.prepareSettingsDTO(promptData, settingsPayload);
    }

    @Override
    protected void afterPrepare(PromptData promptData) {
        log.info("Music Prompt: {}", promptData.getMusicSettings());
    }
}