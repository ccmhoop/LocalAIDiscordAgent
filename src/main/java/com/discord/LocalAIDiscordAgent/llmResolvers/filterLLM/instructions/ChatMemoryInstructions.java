package com.discord.LocalAIDiscordAgent.llmResolvers.filterLLM.instructions;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmResolvers.filterLLM.records.ChatMemoryFilter;

public final class ChatMemoryInstructions {
    
    public static ChatMemoryFilter filterRelevantChatMemory(DiscGlobalData discGlobalData, String summary){
        return ReduceChatMemory.getInstructions(discGlobalData, summary);
    }

}
