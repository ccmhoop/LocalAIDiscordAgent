package com.discord.LocalAIDiscordAgent.resolverLLM.service;

import com.discord.LocalAIDiscordAgent.resolverLLM.calls.ResolverLLMCalls;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResolverLLMService {

    private final ResolverLLMCalls resolverLLMCalls;

    public ResolverLLMService(ResolverLLMCalls ResolverLLMCalls) {
        this.resolverLLMCalls = ResolverLLMCalls;
    }

    public boolean useVectorMemory(String query) {
        return resolverLLMCalls.resolveUseVectorMemory(query);
    }

    public boolean useChatMemory() {
        return resolverLLMCalls.resolveUseChatMemory();
    }

    public boolean useImageGeneration() {
        return resolverLLMCalls.resolveUseImageGeneration();
    }

    public boolean useWebSearch() {
        return resolverLLMCalls.resolveUseWebSearch();
    }

}
