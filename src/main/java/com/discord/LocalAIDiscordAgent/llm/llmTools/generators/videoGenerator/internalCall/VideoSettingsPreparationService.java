package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.videoGenerator.internalCall;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.preparation.GenerationPreparation;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.videoGenerator.payloadRecord.VideoSettingsPayload;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.videoGenerator.validator.VideoPayloadValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VideoSettingsPreparationService extends GenerationPreparation<VideoSettingsPayload> {

    private final VideoSettingCreatePayloadService generationService;
    private final VideoPayloadValidator validator;

    public VideoSettingsPreparationService(
            VideoSettingCreatePayloadService generationService, VideoPayloadValidator validator
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