package com.discord.LocalAIDiscordAgent.resolverLLM.calls;

import com.discord.LocalAIDiscordAgent.resolverLLM.payloads.ResolverChatMemoryPayload;
import com.discord.LocalAIDiscordAgent.resolverLLM.payloads.ResolverImageGenerationPayload;
import com.discord.LocalAIDiscordAgent.resolverLLM.payloads.ResolverVectorMemoryPayload;
import com.discord.LocalAIDiscordAgent.resolverLLM.payloads.ResolverWebSearchPayload;
import com.discord.LocalAIDiscordAgent.resolverLLM.llm.ResolverLLM;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.resolverLLM.records.ResolverLLMPayloadRecord;
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
        ResolverLLMPayloadRecord payload = ResolverChatMemoryPayload.getPayload(discGlobalData);
        return llm.call(payload);
    }

    public boolean resolveUseWebSearch() {
        ResolverLLMPayloadRecord payload = ResolverWebSearchPayload.getPayload(discGlobalData);
        return llm.call(payload);
    }

    public boolean resolveUseImageGeneration() {
        ResolverLLMPayloadRecord payload = ResolverImageGenerationPayload.getPayload();
        return llm.call(payload);
    }

    public boolean resolveUseVectorMemory(String query) {
        List<MergedWebQAItem> vectorDBResults = webQAService.getWebQAResults(query);
        if (vectorDBResults != null) {
            promptData.setRetrievedContext(new VectorDBMemory(vectorDBResults));
            log.debug("Retrieved vectorDB results: {}", promptData.getRetrievedContext());
            ResolverLLMPayloadRecord payload = ResolverVectorMemoryPayload.getPayload(vectorDBResults);
            return llm.call(payload);
        }
        return false;
    }


}
