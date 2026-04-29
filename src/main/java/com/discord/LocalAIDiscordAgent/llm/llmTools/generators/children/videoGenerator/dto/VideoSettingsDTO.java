package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VideoSettingsDTO(
        @JsonProperty(required = true)
        String positivePrompt,
        @JsonProperty(required = true)
        String negativePrompt
){
}
