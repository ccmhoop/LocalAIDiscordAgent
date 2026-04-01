package com.discord.LocalAIDiscordAgent.llmResolvers.structuredLLM.records;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.llmMemory.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;

import java.util.List;

public record StructuredLLMContextRecord(
        List<LongTermMemoryData> longTermMemory,
        RecentMessage lastAssistantMsg
) {
}
