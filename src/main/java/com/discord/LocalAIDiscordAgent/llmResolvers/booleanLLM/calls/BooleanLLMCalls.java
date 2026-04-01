package com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.calls;

import com.discord.LocalAIDiscordAgent.comfyui.llmRequests.booleanLLM.IsImageGenerationRequest;
import com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.llmRequests.booleanLLM.IsChatMemoryRelevantRequest;
import com.discord.LocalAIDiscordAgent.llmMemory.vectorMemory.llmRequests.booleanLLM.IsVectorMemoryRelevantRequest;
import com.discord.LocalAIDiscordAgent.webSearch.llmRequests.resolver.IsWebSearchRequest;
import com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.llm.BooleanLLM;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData.VectorDBMemory;
import com.discord.LocalAIDiscordAgent.llmMemory.vectorMemory.webQAMemory.WebQAService;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public final class BooleanLLMCalls {

    private final PromptData promptData;
    private final WebQAService webQAService;
    private final DiscGlobalData discGlobalData;
    private final BooleanLLM llm;

    public BooleanLLMCalls(
            PromptData promptData,
            BooleanLLM BooleanLLM,
            WebQAService webQAService,
            DiscGlobalData discGlobalData
    ) {
        this.discGlobalData = discGlobalData;
        this.webQAService = webQAService;
        this.promptData = promptData;
        this.llm = BooleanLLM;

    }

    public boolean isChatMemoryRelevant() {
        return llm.call(new IsChatMemoryRelevantRequest(discGlobalData));
    }

    public boolean isWebSearch() {
        return llm.call(new IsWebSearchRequest(discGlobalData));
    }

    public boolean isImageGeneration() {
        return llm.call(new IsImageGenerationRequest());
    }

    public boolean isVectorMemoryRelevant(String query) {
        List<MergedWebQAItem> vectorDBResults = webQAService.getWebQAResults(query);
        if (vectorDBResults != null) {
            promptData.setRetrievedContext(new VectorDBMemory(vectorDBResults));
            log.debug("Retrieved vectorDB results: {}", promptData.getRetrievedContext());
            return llm.call(new IsVectorMemoryRelevantRequest(vectorDBResults));
        }
        return false;
    }

}
