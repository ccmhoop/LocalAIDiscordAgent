package com.discord.LocalAIDiscordAgent.llmQueryGenerator.records;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;

import java.util.List;

public record QueryContextRecord(
        List<LongTermMemoryData> longTermMemory,
        RecentMessage lastAssistantMsg
) {
}
