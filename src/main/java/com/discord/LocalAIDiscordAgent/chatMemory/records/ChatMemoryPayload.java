package com.discord.LocalAIDiscordAgent.chatMemory.records;

import com.discord.LocalAIDiscordAgent.chatMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;

import java.util.List;

public record ChatMemoryPayload(
        List<LongTermMemoryData> longTermMemory,
        GroupMemory groupMemory,
        List<RecentMessage> recentMessages
){}
