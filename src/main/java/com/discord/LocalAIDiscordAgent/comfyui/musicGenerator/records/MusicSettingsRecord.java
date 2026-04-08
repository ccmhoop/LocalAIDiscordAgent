package com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.records;

import com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.records.MusicSettingsRecord.MusicSettingsRecordDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@JsonDeserialize(using = MusicSettingsRecordDeserializer.class)
public record MusicSettingsRecord(
        String tags,
        String lyrics,
        int bpm,
        String keyscale,
        String title
){

    public static class MusicSettingsRecordDeserializer extends JsonDeserializer<MusicSettingsRecord> {
        @Override
        public MusicSettingsRecord deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            String tags = node.path("tags").asText();
            String lyrics = node.path("lyrics").asText();
            int bpm = node.path("bpm").asInt();
            String keyscale = node.path("keyscale").asText();
            String title = node.path("title").asText();
            return new MusicSettingsRecord(
                    tags,
                    lyrics,
                    bpm,
                    keyscale,
                    title
            );

        }
    }
}
