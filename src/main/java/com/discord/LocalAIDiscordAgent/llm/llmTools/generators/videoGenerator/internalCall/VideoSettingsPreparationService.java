package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.videoGenerator.internalCall;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.videoGenerator.payloadRecord.VideoSettingsPayload;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VideoSettingsPreparationService {

    private final VideoSettingCreatePayloadService generationService;

    public VideoSettingsPreparationService(
            VideoSettingCreatePayloadService generationService
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

        VideoSettingsPayload settings = generationService.generatePayload(normalizedUserMessage, normalize(context));
        log.info("Generated video settings: {}", settings);

        promptData.setVideoSettings(normalize(settings));
    }

    private VideoSettingsPayload normalize(VideoSettingsPayload settings) {
        return new VideoSettingsPayload(
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