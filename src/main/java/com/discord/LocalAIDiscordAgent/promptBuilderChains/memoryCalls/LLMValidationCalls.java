package com.discord.LocalAIDiscordAgent.promptBuilderChains.memoryCalls;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llmIsValidChecks.instructions.IsValidInstructions;
import com.discord.LocalAIDiscordAgent.llmIsValidChecks.service.LLMClientIsValidService;
import com.discord.LocalAIDiscordAgent.vectorMemory.webQAMemory.WebQAService;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class LLMValidationCalls {

    private final PromptData promptData;
    private final WebQAService webQAService;
    private final DiscGlobalData discGlobalData;
    private final LLMClientIsValidService LLMClientIsValidService;

    public LLMValidationCalls(
            LLMClientIsValidService LLMClientIsValidService,
            DiscGlobalData discGlobalData,
            WebQAService webQAService,
            PromptData promptData
    ) {
        this.promptData = promptData;
        this.webQAService = webQAService;
        this.discGlobalData = discGlobalData;
        this.LLMClientIsValidService = LLMClientIsValidService;
    }

    public boolean isChatMemoryValid() {
        if (discGlobalData.getRecentMessages() == null || discGlobalData.getRecentMessages().isEmpty()) {
            return false;
        }
        return LLMClientIsValidService.preformCheck(
                IsValidInstructions.checkMessageMemory(discGlobalData)
        );
    }

    public boolean isVectorDBMemoryValid(String query) {
        List<MergedWebQAItem> vectorDBResults = webQAService.getWebQAResults(query);
        if (vectorDBResults != null) {
            promptData.setRetrievedContext(new VectorDBMemory(vectorDBResults));
            log.debug("Retrieved vectorDB results: {}", promptData.getRetrievedContext());
            return LLMClientIsValidService.preformCheck(
                    IsValidInstructions.checkVectorDBMemory(discGlobalData, vectorDBResults)

            );
        }
        return false;
    }

    public record VectorDBMemory(
            List<MergedWebQAItem> vectorDBResults
    ) {
    }

}
