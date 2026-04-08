package com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.musicAdvisor;

import com.discord.LocalAIDiscordAgent.comfyui.imageGenerator.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.records.MusicSettingsRecord;
import com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.validation.MusicSettingsValidator;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MusicSettingsPreparationService {

    private final MusicSettingGenerationService generationService;
//    private final MusicSettingsValidator validator;

    public MusicSettingsPreparationService(
            MusicSettingGenerationService generationService,
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

        MusicSettingsRecord settings = generationService.generate(normalizedUserMessage);
        log.info("Generated music settings: {}", settings);

//        if (!validator.isUsable(settings)) {
//            return;
//        }

        promptData.setMusicSettings(settings);
    }

    private ImageSettingsRecord normalize(ImageSettingsRecord settings) {
        return new ImageSettingsRecord(
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