package com.discord.LocalAIDiscordAgent.textLLM.records;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;

import java.util.List;

public record TextLLMContextRecord(
        List<LongTermMemoryData> longTermMemory,
        RecentMessage lastAssistantMsg
) {

}
