package com.discord.LocalAIDiscordAgent.llmResolvers.structuredLLM.calls;

import com.discord.LocalAIDiscordAgent.comfyui.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmResolvers.structuredLLM.llm.StructuredLLM;
import com.discord.LocalAIDiscordAgent.comfyui.llmRequests.structuredLLM.GenerateImageSettingsRequest;
import com.discord.LocalAIDiscordAgent.queryGenerator.llmRequests.structuredLLM.GenerateVectorQueryRequest;
import com.discord.LocalAIDiscordAgent.queryGenerator.records.QueryRecord;
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
        Record record = llm.call(new GenerateVectorQueryRequest(discGlobalData));
        if (record instanceof QueryRecord(String query)) {
            return query;
        }
        return null;
    }

    public ImageSettingsRecord generateImageSettings() {
        Record record = llm.call(new GenerateImageSettingsRequest());
        if (record instanceof ImageSettingsRecord recordOutPut) {
            return recordOutPut;
        }
        return null;
    }

}
