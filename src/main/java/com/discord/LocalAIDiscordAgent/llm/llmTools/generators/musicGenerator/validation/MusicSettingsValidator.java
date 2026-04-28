package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.musicGenerator.validation;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.musicGenerator.payloadRecord.MusicSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.validator.SettingsValidator;
import org.springframework.stereotype.Component;


@Component
public class MusicSettingsValidator extends SettingsValidator<MusicSettingsPayload> {

    @Override
    public boolean isUsable(MusicSettingsPayload settings) {
     return true;
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