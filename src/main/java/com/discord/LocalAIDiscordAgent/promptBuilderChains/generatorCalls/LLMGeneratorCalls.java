package com.discord.LocalAIDiscordAgent.promptBuilderChains.generatorCalls;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.instructions.QueryGeneratorInstructions;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.service.LLMClientQueryGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LLMGeneratorCalls {

    private final DiscGlobalData discGlobalData;
    private final LLMClientQueryGeneratorService queryGenerator;

    public LLMGeneratorCalls(DiscGlobalData discGlobalData, LLMClientQueryGeneratorService llmClientQueryGeneratorService) {
        this.discGlobalData = discGlobalData;
        this.queryGenerator = llmClientQueryGeneratorService;
    }

    public String chatBasedQuery(){
        return queryGenerator.generateQuery(QueryGeneratorInstructions.generateChatBasedQuery(discGlobalData));
    }
}
