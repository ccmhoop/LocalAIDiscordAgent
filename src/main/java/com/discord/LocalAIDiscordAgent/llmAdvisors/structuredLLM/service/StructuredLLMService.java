package com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.service;

import com.discord.LocalAIDiscordAgent.comfyui.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.calls.StructuredLLMCalls;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StructuredLLMService {

    private final StructuredLLMCalls llm;

    public StructuredLLMService(StructuredLLMCalls structuredLLMCalls) {
        this.llm = structuredLLMCalls;
    }

    public String generateQuery(){
        String query = llm.generateVectorQuery();
        log.info("Generated query: {}", query);
        return query;
    }

    public String generateImageQuery(){
        String query = llm.generateImageQuery();
        log.info("Generated image query: {}", query);
        return query;
    }

    public void summarizeImageContext(){
        String summary = llm.summarizeImageContext();
        log.info("Summarized image context: {}", summary);
    }

    public ImageSettingsRecord generateImageSettings(){
        return llm.generateImageSettings();
    }

}
