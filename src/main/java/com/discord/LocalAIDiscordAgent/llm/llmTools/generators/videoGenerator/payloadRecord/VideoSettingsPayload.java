package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.videoGenerator.payloadRecord;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VideoSettingsPayload(
        @JsonProperty(required = true)
        String positivePrompt,
        @JsonProperty(required = true)
        String negativePrompt
){
}
