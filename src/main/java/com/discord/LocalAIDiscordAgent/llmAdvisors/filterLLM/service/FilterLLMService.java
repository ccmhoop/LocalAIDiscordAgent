package com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.service;

import com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.calls.FilterLLMCalls;
import com.discord.LocalAIDiscordAgent.llmMemory.records.ChatMemoryPayload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FilterLLMService {

    private final FilterLLMCalls filterLLMCalls;

    public FilterLLMService(FilterLLMCalls filterLLMCalls) {
        this.filterLLMCalls = filterLLMCalls;
    }

    public ChatMemoryPayload chatMemoryReducerFilter(boolean useChatMemory){
        log.info("Use Chat Memory: {}", useChatMemory);
        if (!useChatMemory) {
           return new ChatMemoryPayload(null,null, null);
        }
       return filterLLMCalls.removeUnnecessaryChatMemory();
    }

}
