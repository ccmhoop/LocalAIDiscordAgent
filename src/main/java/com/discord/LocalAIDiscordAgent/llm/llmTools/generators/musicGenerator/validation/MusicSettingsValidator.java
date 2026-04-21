package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.musicGenerator.validation;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.imageGenerator.payloadRecord.ImageSettingsPayload;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MusicSettingsValidator {

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

    private String normalizeKeyScale(String keyScale) {
        if (keyScale == null || keyScale.isBlank()) {
            return "C major";
        }

        return switch (keyScale.toLowerCase().trim()) {
            case "c" -> "C major";
            case "d" -> "D major";
            case "e" -> "E major";
            case "f" -> "F major";
            case "g" -> "G major";
            case "a" -> "A major";
            case "b" -> "B major";
            case "cm" -> "C minor";
            case "dm" -> "D minor";
            case "em" -> "E minor";
            case "fm" -> "F minor";
            case "gm" -> "G minor";
            case "am" -> "A minor";
            case "bm" -> "B minor";
            default -> keyScale;
        };
    }
}