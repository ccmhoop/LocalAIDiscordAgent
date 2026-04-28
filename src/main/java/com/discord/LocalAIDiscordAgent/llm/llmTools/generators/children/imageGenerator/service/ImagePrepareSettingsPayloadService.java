package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.service;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.llmCall.ImageGenerateSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.payload.ImageSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.validation.ImageSettingsValidation;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.service.SettingsPayloadPreparation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ImagePrepareSettingsPayloadService extends SettingsPayloadPreparation<ImageSettingsPayload> {

    private final ImageGenerateSettingsPayload generationService;
    private final ImageSettingsValidation validator;

    public ImagePrepareSettingsPayloadService(
            ImageGenerateSettingsPayload generationService,
            ImageSettingsValidation validator
    ) {
        this.generationService = generationService;
        this.validator = validator;
    }

    public void prepare(DiscGlobalData discGlobalData, PromptData promptData) {
        promptData.setImageSettings(
                settingsPreparation(
                        generationService,
                        validator,
                        discGlobalData,
                        promptData
                )
        );
    }

    @Override
    public ImageSettingsPayload normalizeRecord(ImageSettingsPayload settings) {
        return new ImageSettingsPayload(
                normalize(settings.positivePrompt()),
                normalize(settings.negativePrompt()),
                settings.pixelWidth(),
                settings.pixelHeight()
        );
    }

}