package com.discord.LocalAIDiscordAgent.comfyui.videoGenerator.videoAdvisor;

import com.discord.LocalAIDiscordAgent.comfyui.videoGenerator.records.VideoSettingsRecord;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VideoSettingsPreparationService {

    private final VideoSettingGenerationService generationService;

    public VideoSettingsPreparationService(
            VideoSettingGenerationService generationService
    ) {
        this.generationService = generationService;
    }

    public void prepare(DiscGlobalData discGlobalData, PromptData promptData) {
        String userMessage = discGlobalData.getUserMessage();
        String normalizedUserMessage = normalize(userMessage);

        if (normalizedUserMessage == null) {
            return;
        }
        String context = promptData.getSummary();

        VideoSettingsRecord settings = generationService.generate(normalizedUserMessage, normalize(context));
        log.info("Generated video settings: {}", settings);

        promptData.setVideoSettings(normalize(settings));
    }

    private VideoSettingsRecord normalize(VideoSettingsRecord settings) {
        return new VideoSettingsRecord(
                normalize(settings.positivePrompt()),
                normalize(settings.negativePrompt())
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}