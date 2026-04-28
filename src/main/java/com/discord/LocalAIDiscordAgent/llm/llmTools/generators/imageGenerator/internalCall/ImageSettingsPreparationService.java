package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.imageGenerator.internalCall;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.imageGenerator.payloadRecord.ImageSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.imageGenerator.validation.ImageSettingsValidator;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.preparation.GenerationPreparation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ImageSettingsPreparationService extends GenerationPreparation<ImageSettingsPayload> {

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