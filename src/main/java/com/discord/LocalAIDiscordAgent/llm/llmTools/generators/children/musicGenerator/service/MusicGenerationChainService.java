package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.service;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.toolCalls.LLMToolCalls;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.llmGenerate.MusicSettingsLLMGenerate;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.dto.MusicSettingsDTO;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.fileGeneration.MusicFileGeneration;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.preparation.MusicGenerationPreparation;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.service.FileGenerationChain;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class MusicGenerationChainService extends FileGenerationChain<MusicSettingsDTO> {

    private final MusicGenerationPreparation preparation;
    private final MusicSettingsLLMGenerate llmGeneration;
    private final MusicFileGeneration fileGeneration;

    public MusicGenerationChainService(
            MapperUtils mapperUtils,
            MusicGenerationPreparation preparation,
            LLMToolCalls LLMToolCalls, MusicSettingsLLMGenerate llmGeneration, MusicFileGeneration fileGeneration
    ) {
        super(mapperUtils, LLMToolCalls);
        this.preparation = preparation;
        this.llmGeneration = llmGeneration;
        this.fileGeneration = fileGeneration;
    }

    @Override
    public Mono<ComfyuiService.GeneratedFile> executeLLMChain(
            DiscGlobalData discGlobalData,
            boolean requiresContext
    ) {
        return executeChain(discGlobalData, requiresContext, "music");
    }

    @Override
    protected MusicSettingsDTO llmGenerateFileSettings(String userMessage, String context) {
        return llmGeneration.generateFileSettings(userMessage, context);
    }

    @Override
    protected Mono<ComfyuiService.GeneratedFile> generateFile(PromptData promptData) {
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