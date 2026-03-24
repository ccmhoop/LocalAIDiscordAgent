package com.discord.LocalAIDiscordAgent.promptBuilderChains.llmCallChains;

import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.generatorCalls.LLMGeneratorCalls;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.memoryCalls.LLMValidationCalls;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.toolCalls.LLMToolCalls;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LLMCallChain {

    private final PromptData promptData;
    private final LLMToolCalls LLMToolCalls;
    private final LLMValidationCalls LLMValidationCalls;
    private final LLMGeneratorCalls LLMGeneratorCalls;

    public LLMCallChain(
            LLMValidationCalls LLMValidationCalls,
            LLMToolCalls LLMToolCalls,
            PromptData promptData, LLMGeneratorCalls llmGeneratorCalls
    ) {
        this.promptData = promptData;
        this.LLMToolCalls = LLMToolCalls;
        this.LLMValidationCalls = LLMValidationCalls;
        LLMGeneratorCalls = llmGeneratorCalls;
    }

    public String executeContextChain() {
//        boolean useChatMemory = promptMemoryCalls.isChatMemoryRelevant();

        String query = LLMGeneratorCalls.chatBasedQuery();

        log.info("Query: {}", query);

        boolean useVectorDbMemory = LLMValidationCalls.isVectorDBMemoryValid(query);

        log.info("Use Vector DB Memory: {}", useVectorDbMemory);

        if (!useVectorDbMemory) {
            LLMToolCalls.callWebSearchTool();
        }

        if (promptData.isRetrievedContextPresent()) {
         return LLMToolCalls.callSummaryTool();
        }

        return null;
    }
}
