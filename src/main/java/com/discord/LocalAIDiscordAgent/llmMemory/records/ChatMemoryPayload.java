package com.discord.LocalAIDiscordAgent.llmMemory.records;

import com.discord.LocalAIDiscordAgent.llmMemory.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;

import java.util.List;

public record ChatMemoryPayload(
        List<LongTermMemoryData> longTermMemory,
        GroupMemory groupMemory,
        List<RecentMessage> recentMessages
){}
