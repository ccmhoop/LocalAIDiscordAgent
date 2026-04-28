package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.service;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.llmCall.MusicGenerateSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.payload.MusicSettingsPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.validation.MusicSettingsValidation;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.service.SettingsPayloadPreparation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MusicPrepareSettingsPayloadService extends SettingsPayloadPreparation<MusicSettingsPayload> {

    private final MusicGenerateSettingsPayload generationService;
    private final MusicSettingsValidation validator;

    public MusicPrepareSettingsPayloadService(
            MusicGenerateSettingsPayload generationService,
            MusicSettingsValidation validator
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