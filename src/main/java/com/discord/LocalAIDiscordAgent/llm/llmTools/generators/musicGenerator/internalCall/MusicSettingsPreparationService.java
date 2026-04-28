package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.musicGenerator.internalCall;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.musicGenerator.payloadRecord.MusicSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.musicGenerator.validation.MusicSettingsValidator;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.preparation.GenerationPreparation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MusicSettingsPreparationService extends GenerationPreparation<MusicSettingsPayload> {

    private final MusicSettingCreatePayloadService generationService;
    private final MusicSettingsValidator validator;

    public MusicSettingsPreparationService(
            MusicSettingCreatePayloadService generationService,
            MusicSettingsValidator validator
    ) {
        this.generationService = generationService;
        this.validator = validator;
    }

    public void prepare(DiscGlobalData discGlobalData, PromptData promptData) {
        promptData.setMusicSettings(
                settingsPreparation(
                        generationService,
                        validator,
                        discGlobalData,
                        promptData
                )
        );
    }

    @Override
    public MusicSettingsPayload normalizeRecord(MusicSettingsPayload settings) {
        return new MusicSettingsPayload(
                normalize(settings.tags()),
                normalize(settings.lyrics()),
                settings.bpm(),
                settings.keyscale(),
                settings.duration(),
                normalize(settings.title())
        );
    }


}