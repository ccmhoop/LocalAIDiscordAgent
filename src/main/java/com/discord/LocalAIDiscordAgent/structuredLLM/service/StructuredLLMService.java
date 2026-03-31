package com.discord.LocalAIDiscordAgent.structuredLLM.service;

import com.discord.LocalAIDiscordAgent.comfyui.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.structuredLLM.calls.StructuredLLMCalls;
import org.springframework.stereotype.Service;

@Service
public class StructuredLLMService {

    private final StructuredLLMCalls llm;

    public StructuredLLMService(StructuredLLMCalls structuredLLMCalls) {
        this.llm = structuredLLMCalls;
    }

    public String generateQuery(){
        return llm.generateVectorQuery();
    }

    public ImageSettingsRecord generateImageSettings(){
        return llm.generateImageSettings();
    }

}
