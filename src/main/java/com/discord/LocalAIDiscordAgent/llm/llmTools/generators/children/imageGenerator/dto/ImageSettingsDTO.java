package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ImageSettingsDTO(
        @JsonProperty(required = true) String positivePrompt,
        @JsonProperty(required = true) String negativePrompt,
        @JsonProperty(required = true) int pixelWidth,
        @JsonProperty(required = true) int pixelHeight
){}