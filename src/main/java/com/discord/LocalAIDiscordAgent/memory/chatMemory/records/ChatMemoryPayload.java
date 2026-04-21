package com.discord.LocalAIDiscordAgent.memory.chatMemory.records;

import com.discord.LocalAIDiscordAgent.memory.chatMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.RecentMessage;

import java.util.List;

public record ChatMemoryPayload(
        List<LongTermMemoryData> longTermMemory,
        GroupMemory groupMemory,
        List<RecentMessage> recentMessages
){}
