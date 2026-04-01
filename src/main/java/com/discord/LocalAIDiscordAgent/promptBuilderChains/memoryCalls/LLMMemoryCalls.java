package com.discord.LocalAIDiscordAgent.promptBuilderChains.memoryCalls;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmResolvers.filterLLM.instructions.ChatMemoryInstructions;
import com.discord.LocalAIDiscordAgent.llmResolvers.filterLLM.service.LLMClientChatMemoryFilterService;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LLMMemoryCalls {

    private final DiscGlobalData discGlobalData;
    private final PromptData promptData;
    private final LLMClientChatMemoryFilterService llmClientChatMemoryFilterService;

    public LLMMemoryCalls(DiscGlobalData discGlobalData, PromptData promptData, LLMClientChatMemoryFilterService llmClientChatMemoryFilterService) {
        this.discGlobalData = discGlobalData;
        this.promptData = promptData;
        this.llmClientChatMemoryFilterService = llmClientChatMemoryFilterService;
    }

    public RuntimeContext filterChatMemory() {
        if (discGlobalData.getRecentMessages() == null) {
            return new RuntimeContext(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
        String context = null;
        if (promptData.getRetrievedContext() == null) {
            context = promptData.getRetrievedContext();
        }
        return llmClientChatMemoryFilterService.preformFilter(ChatMemoryInstructions.filterRelevantChatMemory(discGlobalData, context));
    }
}
