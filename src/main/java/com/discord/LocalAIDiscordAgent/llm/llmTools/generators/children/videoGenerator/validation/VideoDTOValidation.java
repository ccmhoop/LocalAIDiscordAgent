package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.validation;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.validation.FileDTOValidation;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.dto.VideoSettingsDTO;
import org.springframework.stereotype.Component;

@Component
public class VideoDTOValidation extends FileDTOValidation<VideoSettingsDTO> {

    @Override
    public boolean isUsable(VideoSettingsDTO settings) {
        return true;
    }

}
