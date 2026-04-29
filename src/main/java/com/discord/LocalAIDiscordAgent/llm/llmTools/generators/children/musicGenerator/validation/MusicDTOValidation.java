package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.validation;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.dto.MusicSettingsDTO;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.validation.FileDTOValidation;
import org.springframework.stereotype.Component;

@Component
public class MusicDTOValidation extends FileDTOValidation<MusicSettingsDTO> {

    @Override
    public boolean isUsable(MusicSettingsDTO settings) {
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