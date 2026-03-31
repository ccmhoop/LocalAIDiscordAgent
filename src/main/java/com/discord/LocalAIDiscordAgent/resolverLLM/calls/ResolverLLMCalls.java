package com.discord.LocalAIDiscordAgent.resolverLLM.calls;

import com.discord.LocalAIDiscordAgent.comfyui.llmRequests.resolver.ImageGenerationRequest;
import com.discord.LocalAIDiscordAgent.chatMemory.ResolveRequests.IsChatMemoryRelevant;
import com.discord.LocalAIDiscordAgent.vectorMemory.llmRequests.resolver.IsVectorMemoryRelevantRequest;
import com.discord.LocalAIDiscordAgent.webSearch.llmRequests.resolver.IsWebSearchRequest;
import com.discord.LocalAIDiscordAgent.resolverLLM.llm.ResolverLLM;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData.VectorDBMemory;
import com.discord.LocalAIDiscordAgent.vectorMemory.webQAMemory.WebQAService;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public final class ResolverLLMCalls {

    private final PromptData promptData;
    private final WebQAService webQAService;
    private final DiscGlobalData discGlobalData;
    private final ResolverLLM llm;

    public ResolverLLMCalls(
            PromptData promptData,
            ResolverLLM ResolverLLM,
            WebQAService webQAService,
            DiscGlobalData discGlobalData
    ) {
        this.discGlobalData = discGlobalData;
        this.webQAService = webQAService;
        this.promptData = promptData;
        this.llm = ResolverLLM;

    }

    public boolean resolveUseChatMemory() {
        return llm.call(new IsChatMemoryRelevant(discGlobalData));
    }

    public boolean resolveUseWebSearch() {
        return llm.call(new IsWebSearchRequest(discGlobalData));
    }

    public boolean resolveUseImageGeneration() {
        return llm.call(new ImageGenerationRequest());
    }

    public boolean resolveUseVectorMemory(String query) {
        List<MergedWebQAItem> vectorDBResults = webQAService.getWebQAResults(query);
        if (vectorDBResults != null) {
            promptData.setRetrievedContext(new VectorDBMemory(vectorDBResults));
            log.debug("Retrieved vectorDB results: {}", promptData.getRetrievedContext());
            return llm.call(new IsVectorMemoryRelevantRequest(vectorDBResults));
        }
        return false;
    }


}
