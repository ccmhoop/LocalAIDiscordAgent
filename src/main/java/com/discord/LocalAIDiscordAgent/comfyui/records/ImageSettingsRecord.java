package com.discord.LocalAIDiscordAgent.comfyui.records;

import com.discord.LocalAIDiscordAgent.comfyui.records.ImageSettingsRecord.ImageSettingsRecordDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@JsonDeserialize(using = ImageSettingsRecordDeserializer.class)
public record ImageSettingsRecord(
        String positivePrompt,
        String negativePrompt,
        int pixelWidth,
        int pixelHeight
) {
    public static class ImageSettingsRecordDeserializer extends JsonDeserializer<ImageSettingsRecord> {
        @Override
        public ImageSettingsRecord deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            String positivePrompt = node.path("positivePrompt").asText();
            String negativePrompt = node.path("negativePrompt").asText();
            int pixelWidth = node.path("pixelWidth").asInt();
            int pixelHeight = node.path("pixelHeight").asInt();

            return new ImageSettingsRecord(
                    positivePrompt,
                    negativePrompt,
                    pixelWidth,
                    pixelHeight
            );

        }
    }
}
