package com.discord.LocalAIDiscordAgent.vectorMemory.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize
public record QueryRecord(
        @JsonProperty(required = true, value = "query")
        String query
) {
}
