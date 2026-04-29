package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.preparation;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.dto.ImageSettingsDTO;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.validation.ImageDTOValidation;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.preparation.FileGenerationPreparation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ImageGenerationPreparation extends FileGenerationPreparation<ImageSettingsDTO> {

    private final ImageDTOValidation validator;

    public ImageGenerationPreparation(ImageDTOValidation validator) {
        this.validator = validator;
    }

    public void prepareSettingsDTO(PromptData promptData, ImageSettingsDTO settingsPayload) {
        promptData.setImageSettingsDTO(settingsPreparation(validator, settingsPayload));
    }

    @Override
    public ImageSettingsDTO normalizeRecord(ImageSettingsDTO settings) {
        return new ImageSettingsDTO(
                normalize(settings.positivePrompt()),
                normalize(settings.negativePrompt()),
                settings.pixelWidth(),
                settings.pixelHeight()
        );
    }

}