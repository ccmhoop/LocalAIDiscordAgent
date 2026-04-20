package com.discord.LocalAIDiscordAgent.comfyui.generators.imageGenerator.payloadRecord;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ImageSettingsPayload(
        @JsonProperty(required = true) String positivePrompt,
        @JsonProperty(required = true) String negativePrompt,
        @JsonProperty(required = true) int pixelWidth,
        @JsonProperty(required = true) int pixelHeight
){}