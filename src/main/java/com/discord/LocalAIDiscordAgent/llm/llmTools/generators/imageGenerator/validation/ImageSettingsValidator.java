package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.imageGenerator.validation;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.imageGenerator.payloadRecord.ImageSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.validator.SettingsValidator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ImageSettingsValidator extends SettingsValidator<ImageSettingsPayload> {

    private static final Set<String> ALLOWED_RESOLUTIONS = Set.of(
            "1024x1024",
            "1152x896",
            "896x1152",
            "1216x832",
            "832x1216",
            "1344x768",
            "768x1344",
            "1536x640",
            "640x1536"
    );

    @Override
    public boolean isUsable(ImageSettingsPayload settings) {
        if (settings == null) {
            return false;
        }

        if (isBlank(settings.positivePrompt())) {
            return false;
        }

        if (isBlank(settings.negativePrompt())) {
            return false;
        }

        return ALLOWED_RESOLUTIONS.contains(key(settings.pixelWidth(), settings.pixelHeight()));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private String key(int width, int height) {
        return width + "x" + height;
    }
}