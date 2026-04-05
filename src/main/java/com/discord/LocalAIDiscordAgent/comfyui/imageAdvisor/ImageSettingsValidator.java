package com.discord.LocalAIDiscordAgent.comfyui.imageAdvisor;

import com.discord.LocalAIDiscordAgent.comfyui.records.ImageSettingsRecord;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ImageSettingsValidator {

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

    public boolean isUsable(ImageSettingsRecord settings) {
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