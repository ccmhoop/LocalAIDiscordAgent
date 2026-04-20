package com.discord.LocalAIDiscordAgent.comfyui.generators.musicGenerator.payloadRecord;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MusicSettingsPayload(
        @JsonProperty(required = true) String tags,
        @JsonProperty(required = true) String lyrics,
        @JsonProperty(required = true) int bpm,
        @JsonProperty(required = true) String keyscale,
        @JsonProperty(required = true) double duration,
        @JsonProperty(required = true) String title
){
}
