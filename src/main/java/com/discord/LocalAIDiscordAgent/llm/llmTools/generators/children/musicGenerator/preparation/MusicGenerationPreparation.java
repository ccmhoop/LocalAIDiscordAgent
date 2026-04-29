package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.preparation;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.dto.MusicSettingsDTO;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.validation.MusicDTOValidation;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.preparation.FileGenerationPreparation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MusicGenerationPreparation extends FileGenerationPreparation<MusicSettingsDTO> {

    private final MusicDTOValidation validator;

    public MusicGenerationPreparation(MusicDTOValidation validator) {
        this.validator = validator;
    }

    public void prepareSettingsDTO(PromptData promptData, MusicSettingsDTO settingsPayload) {
        promptData.setMusicSettings(settingsPreparation(validator, settingsPayload));
    }

    @Override
    public MusicSettingsDTO normalizeRecord(MusicSettingsDTO settings) {
        return new MusicSettingsDTO(
                normalize(settings.tags()),
                normalize(settings.lyrics()),
                settings.bpm(),
                settings.keyscale(),
                settings.duration(),
                normalize(settings.title())
        );
    }


}