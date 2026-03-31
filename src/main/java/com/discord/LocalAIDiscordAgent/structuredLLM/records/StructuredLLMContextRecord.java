package com.discord.LocalAIDiscordAgent.structuredLLM.records;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;

import java.util.List;

public record StructuredLLMContextRecord(
        List<LongTermMemoryData> longTermMemory,
        RecentMessage lastAssistantMsg
) {
}
