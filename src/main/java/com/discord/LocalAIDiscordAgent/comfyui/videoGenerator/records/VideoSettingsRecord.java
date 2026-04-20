package com.discord.LocalAIDiscordAgent.comfyui.videoGenerator.records;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VideoSettingsRecord (
        @JsonProperty(required = true)
        String positivePrompt,
        @JsonProperty(required = true)
        String negativePrompt
){
}
