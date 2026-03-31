package com.discord.LocalAIDiscordAgent.filterLLM.records;


import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;

import java.util.List;

public record ChatMemoryContext(
        String documentSummary,
        List<LongTermMemoryData> longTermMemory,
        GroupMemory groupMemory,
        List<RecentMessage> recentMessages
) {
}
