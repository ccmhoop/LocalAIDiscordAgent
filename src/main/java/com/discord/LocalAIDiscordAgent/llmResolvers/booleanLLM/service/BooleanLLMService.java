package com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.service;

import com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.calls.BooleanLLMCalls;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BooleanLLMService {

    private final BooleanLLMCalls booleanLLMCalls;

    public BooleanLLMService(BooleanLLMCalls BooleanLLMCalls) {
        this.booleanLLMCalls = BooleanLLMCalls;
    }

    public boolean useVectorMemory(String query) {
        return booleanLLMCalls.isVectorMemoryRelevant(query);
    }

    public boolean useChatMemory() {
        return booleanLLMCalls.isChatMemoryRelevant();
    }

    public boolean useImageGeneration() {
        return booleanLLMCalls.isImageGeneration();
    }

    public boolean useWebSearch() {
        return booleanLLMCalls.isWebSearch();
    }

}
