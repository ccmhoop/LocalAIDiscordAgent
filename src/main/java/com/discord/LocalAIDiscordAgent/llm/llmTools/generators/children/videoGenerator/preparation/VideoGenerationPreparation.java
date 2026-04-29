package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.preparation;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.preparation.FileGenerationPreparation;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.dto.VideoSettingsDTO;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.validation.VideoDTOValidation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VideoGenerationPreparation extends FileGenerationPreparation<VideoSettingsDTO> {

    private final VideoDTOValidation validator;

    public VideoGenerationPreparation(VideoDTOValidation validator) {
        this.validator = validator;
    }

    public void prepareSettingsDTO(PromptData promptData, VideoSettingsDTO settingsPayload) {
        promptData.setVideoSettings(
                settingsPreparation(
                        validator,
                        settingsPayload
                )
            );
    }

    @Override
    public VideoSettingsDTO normalizeRecord(VideoSettingsDTO settings) {
        return new VideoSettingsDTO(
                normalize(settings.positivePrompt()),
                normalize(settings.negativePrompt())
        );
    }

}