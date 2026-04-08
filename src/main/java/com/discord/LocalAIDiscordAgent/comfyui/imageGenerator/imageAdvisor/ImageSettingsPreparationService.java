package com.discord.LocalAIDiscordAgent.comfyui.imageGenerator.imageAdvisor;

import com.discord.LocalAIDiscordAgent.comfyui.imageGenerator.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ImageSettingsPreparationService {

    private final ImageSettingGenerationService generationService;
    private final ImageSettingsValidator validator;

    public ImageSettingsPreparationService(
            ImageSettingGenerationService generationService,
            ImageSettingsValidator validator
    ) {
        this.generationService = generationService;
        this.validator = validator;
    }

    public void prepare(DiscGlobalData discGlobalData, PromptData promptData) {
        String userMessage = discGlobalData.getUserMessage();
        String normalizedUserMessage = normalize(userMessage);

        if (normalizedUserMessage == null) {
            return;
        }
        String context = promptData.getSummary();

        ImageSettingsRecord settings = generationService.generate(normalizedUserMessage, normalize(context));
        log.info("Generated image settings: {}", settings);

        if (!validator.isUsable(settings)) {
            return;
        }

        promptData.setImageSettings(normalize(settings));
    }

    private ImageSettingsRecord normalize(ImageSettingsRecord settings) {
        return new ImageSettingsRecord(
                normalize(settings.positivePrompt()),
                normalize(settings.negativePrompt()),
                settings.pixelWidth(),
                settings.pixelHeight()
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