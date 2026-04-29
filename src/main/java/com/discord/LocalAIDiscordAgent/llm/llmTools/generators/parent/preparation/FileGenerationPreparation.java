package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.preparation;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.validation.FileDTOValidation;

public abstract class FileGenerationPreparation<T extends Record>  {

    public String prepareUserMessage(DiscGlobalData discGlobalData) {
        String userMessage = discGlobalData.getUserMessage();
        return normalize(userMessage);
    }

    public T settingsPreparation(FileDTOValidation<T> validator, T settings){
            if (!validator.isUsable(settings)) {
                return null;
            }
            return normalizeRecord(settings);
    }

    public String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public abstract void prepareSettingsDTO(PromptData promptData, T settingsPayload);
    public abstract T normalizeRecord(T settings);


}
