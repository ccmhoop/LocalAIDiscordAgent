package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.videoGenerator.validator;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.validator.SettingsValidator;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.videoGenerator.payloadRecord.VideoSettingsPayload;
import org.springframework.stereotype.Component;

@Component
public class VideoPayloadValidator extends SettingsValidator<VideoSettingsPayload> {

    @Override
    public boolean isUsable(VideoSettingsPayload settings) {
        return true;
    }

}
