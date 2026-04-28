package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.validation;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.validation.SettingsPayloadValidator;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.payload.VideoSettingsPayload;
import org.springframework.stereotype.Component;

@Component
public class VideoSettingsValidation extends SettingsPayloadValidator<VideoSettingsPayload> {

    @Override
    public boolean isUsable(VideoSettingsPayload settings) {
        return true;
    }

}
