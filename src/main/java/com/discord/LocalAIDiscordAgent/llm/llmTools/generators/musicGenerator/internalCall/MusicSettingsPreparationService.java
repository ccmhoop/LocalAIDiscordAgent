package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.musicGenerator.internalCall;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.imageGenerator.payloadRecord.ImageSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.musicGenerator.payloadRecord.MusicSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.musicGenerator.validation.MusicSettingsValidator;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MusicSettingsPreparationService {

    private final MusicSettingCreatePayloadService generationService;
//    private final MusicSettingsValidator validator;

    public MusicSettingsPreparationService(
            MusicSettingCreatePayloadService generationService,
            MusicSettingsValidator validator
    ) {
        this.generationService = generationService;
//        this.validator = validator;
    }

    public void prepare(DiscGlobalData discGlobalData, PromptData promptData) {
        String userMessage = discGlobalData.getUserMessage();
        String normalizedUserMessage = normalize(userMessage);

        if (normalizedUserMessage == null) {
            return;
        }
//        String context = promptData.getSummary();

        MusicSettingsPayload settings = generationService.generatePayload(normalizedUserMessage, "");
        log.info("Generated music settings: {}", settings);

//        if (!validator.isUsable(settings)) {
//            return;
//        }

        promptData.setMusicSettings(settings);
    }

    private ImageSettingsPayload normalize(ImageSettingsPayload settings) {
        return new ImageSettingsPayload(
                normalize(settings.positivePrompt()),
                normalize(settings.negativePrompt()),
                settings.pixelWidth(),
                settings.pixelHeight()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}