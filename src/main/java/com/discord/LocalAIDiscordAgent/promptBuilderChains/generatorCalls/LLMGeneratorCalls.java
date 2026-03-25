package com.discord.LocalAIDiscordAgent.promptBuilderChains.generatorCalls;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.instructions.QueryGeneratorInstructions;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.service.LLMClientQueryGeneratorService;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.service.LLMClientQueryGeneratorService.ImagePromptOutput;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.service.LLMClientQueryGeneratorService.LLMQueryGenerationRecord;
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
        Record record =  queryGenerator.generateQuery(QueryGeneratorInstructions.generateChatBasedQuery(discGlobalData));
        if (record instanceof LLMQueryGenerationRecord(String query)) {
            return query;
        }
        return null;
    }

    public ImagePromptOutput imagePrompt(){
        Record record =  queryGenerator.generateQuery(QueryGeneratorInstructions.generateImagePrompt());
        if(record instanceof ImagePromptOutput recordOutPut){
            return recordOutPut;
        }
        return null;
    }
}
