package com.discord.LocalAIDiscordAgent.llmIsValidChecks.instructions;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmIsValidChecks.records.IsValidRecord;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;

import java.util.List;

public final class IsValidInstructions {

    public static IsValidRecord checkMessageMemory(DiscGlobalData discGlobalData) {
        return IsChatMemoryValid.getInstructions(discGlobalData);
    }

    public static IsValidRecord checkVectorDBMemory(DiscGlobalData discGlobalData, List<MergedWebQAItem> vectorDBMemory) {
        return IsVectorMemoryValid.getInstructions(vectorDBMemory, discGlobalData.getLastAssistantMsg());
    }

}
