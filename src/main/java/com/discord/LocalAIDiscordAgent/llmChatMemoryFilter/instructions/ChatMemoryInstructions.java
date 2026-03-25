package com.discord.LocalAIDiscordAgent.llmChatMemoryFilter.instructions;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmChatMemoryFilter.records.ChatMemoryFilter;

public final class ChatMemoryInstructions {
    
    public static ChatMemoryFilter filterRelevantChatMemory(DiscGlobalData discGlobalData, String summary){
        return ReduceChatMemory.getInstructions(discGlobalData, summary);
    }

}
