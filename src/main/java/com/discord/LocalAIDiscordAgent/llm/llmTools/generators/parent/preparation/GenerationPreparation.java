package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.preparation;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.generator.LLMSettingsGenerator;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.validator.SettingsValidator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class GenerationPreparation <T extends Record>  {

    public T settingsPreparation(
            LLMSettingsGenerator<T> generationService,
            SettingsValidator<T> validator,
            DiscGlobalData discGlobalData,
            PromptData promptData
    ){
            String userMessage = discGlobalData.getUserMessage();
            String normalizedUserMessage = normalize(userMessage);

            if (normalizedUserMessage == null) {
                return null;
            }

            String context = promptData.getSummary();

            T settings = generationService.generatePayload(normalizedUserMessage, normalize(context));
            log.info("Generated image settings: {}", settings);

            if (!validator.isUsable(settings)) {
                return null;
            }

            return normalizeRecord(settings);
    }


    public abstract T normalizeRecord(T settings);

    public String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

}
