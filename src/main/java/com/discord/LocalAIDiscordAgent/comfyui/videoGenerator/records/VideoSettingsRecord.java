package com.discord.LocalAIDiscordAgent.comfyui.videoGenerator.records;

import com.discord.LocalAIDiscordAgent.comfyui.videoGenerator.records.VideoSettingsRecord.VideoSettingsRecordDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@JsonDeserialize(using = VideoSettingsRecordDeserializer.class)
public record VideoSettingsRecord (
        String positivePrompt,
        String negativePrompt
){

    public static class VideoSettingsRecordDeserializer extends JsonDeserializer<VideoSettingsRecord> {
        @Override
        public VideoSettingsRecord deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            String positivePrompt = node.path("positivePrompt").asText();
            String negativePrompt = node.path("negativePrompt").asText();
            return new VideoSettingsRecord(
                    positivePrompt,
                    negativePrompt
            );

        }
    }
}
