package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.imageGenerator.internalCall;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.imageGenerator.payloadRecord.ImageSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.imageGenerator.validation.ImageSettingsValidator;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ImageSettingsPreparationService {

    private final ImageSettingCreatePayloadService generationService;
    private final ImageSettingsValidator validator;

    public ImageSettingsPreparationService(
            ImageSettingCreatePayloadService generationService,
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

        ImageSettingsPayload settings = generationService.generatePayload(normalizedUserMessage, normalize(context));
        log.info("Generated image settings: {}", settings);

        if (!validator.isUsable(settings)) {
            return;
        }

        promptData.setImageSettings(normalize(settings));
    }

    private ImageSettingsPayload normalize(ImageSettingsPayload settings) {
        return new ImageSettingsPayload(
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