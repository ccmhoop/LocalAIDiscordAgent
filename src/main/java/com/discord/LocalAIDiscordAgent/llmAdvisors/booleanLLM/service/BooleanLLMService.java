package com.discord.LocalAIDiscordAgent.llmAdvisors.booleanLLM.service;

import com.discord.LocalAIDiscordAgent.llmAdvisors.booleanLLM.calls.BooleanLLMCalls;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BooleanLLMService {

    private final BooleanLLMCalls booleanLLMCalls;

    public BooleanLLMService(BooleanLLMCalls BooleanLLMCalls) {
        this.booleanLLMCalls = BooleanLLMCalls;
    }

    public boolean useVectorMemoryIfRelevant(String query) {
        boolean useVectorMemory = booleanLLMCalls.isVectorMemoryRelevant(query);
        log.info("Use Vector Memory: {}", useVectorMemory);
        return useVectorMemory;
    }

    public boolean useChatMemoryIfRelevant() {
        return booleanLLMCalls.isChatMemoryRelevant();
    }

    public boolean useImageGenerationIfRequested() {
        boolean useImageGeneration = booleanLLMCalls.isImageGeneration();
        log.info("Use Image Generation: {}", useImageGeneration);
        return useImageGeneration;
    }

    public boolean useImageContextIfValid(String query) {
        boolean useImageContext = booleanLLMCalls.isImageContextValid(query);
        log.info("Use Image Context: {}", useImageContext);
        return useImageContext;
    }

    public boolean useWebSearchIfRequired() {
        boolean useWebSearch = booleanLLMCalls.isWebSearch();
        log.info("Use Web Search: {}", useWebSearch);
        return useWebSearch;
    }

}
