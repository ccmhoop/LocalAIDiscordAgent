package com.discord.LocalAIDiscordAgent.structuredLLM.calls;

import com.discord.LocalAIDiscordAgent.comfyui.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.structuredLLM.llm.StructuredLLM;
import com.discord.LocalAIDiscordAgent.structuredLLM.payload.StructuredImageSettingPayload;
import com.discord.LocalAIDiscordAgent.structuredLLM.payload.StructuredVectorQueryPayload;
import com.discord.LocalAIDiscordAgent.structuredLLM.records.StructuredLLMPayloadRecord;
import com.discord.LocalAIDiscordAgent.vectorMemory.records.QueryRecord;
import org.springframework.stereotype.Component;

@Component
public class StructuredLLMCalls {

    private final StructuredLLM llm;
    private final DiscGlobalData discGlobalData;

    public StructuredLLMCalls(
            StructuredLLM structuredLLM,
            DiscGlobalData discGlobalData
    ) {
        this.discGlobalData = discGlobalData;
        this.llm = structuredLLM;
    }

    public String generateVectorQuery() {
        StructuredLLMPayloadRecord payload = StructuredVectorQueryPayload.getPayload(discGlobalData);
        Record record = llm.call(payload, QueryRecord.class);
        if (record instanceof QueryRecord(String query)) {
            return query;
        }
        return null;
    }

    public ImageSettingsRecord generateImageGenerationSettings() {
        StructuredLLMPayloadRecord payload = StructuredImageSettingPayload.getPayload();
        Record record = llm.call(payload, ImageSettingsRecord.class);
        if (record instanceof ImageSettingsRecord recordOutPut) {
            return recordOutPut;
        }
        return null;
    }

}
