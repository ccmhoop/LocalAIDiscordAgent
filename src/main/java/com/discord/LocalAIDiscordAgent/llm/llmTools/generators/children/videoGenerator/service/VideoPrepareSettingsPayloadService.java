package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.service;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.service.SettingsPayloadPreparation;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.llmCall.VideoGenerateSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.payload.VideoSettingsPayload;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.validation.VideoSettingsValidation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VideoPrepareSettingsPayloadService extends SettingsPayloadPreparation<VideoSettingsPayload> {

    private final VideoGenerateSettingsPayload generationService;
    private final VideoSettingsValidation validator;

    public VideoPrepareSettingsPayloadService(
            VideoGenerateSettingsPayload generationService, VideoSettingsValidation validator
    ) {
        this.generationService = generationService;
        this.validator = validator;
    }

    public void prepare(DiscGlobalData discGlobalData, PromptData promptData) {
        promptData.setVideoSettings(
                settingsPreparation(
                        generationService,
                        validator,
                        discGlobalData,
                        promptData
                )
            );
    }

    @Override
    public VideoSettingsPayload normalizeRecord(VideoSettingsPayload settings) {
        return new VideoSettingsPayload(
                normalize(settings.positivePrompt()),
                normalize(settings.negativePrompt())
        );
    }

}