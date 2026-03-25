package com.discord.LocalAIDiscordAgent.llmQueryGenerator.instructions;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.records.QueryGeneratorRecord;

public final class QueryGeneratorInstructions {

    public static QueryGeneratorRecord generateChatBasedQuery(DiscGlobalData discGlobalData){
       return QueryVectorMemory.getInstructions(discGlobalData);
    }

    public static QueryGeneratorRecord generateImagePrompt(){
        return ImagePrompt.getInstructions();
    }

}
