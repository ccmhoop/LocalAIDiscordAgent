package com.discord.LocalAIDiscordAgent.chatMemory.records;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;

import java.util.List;

public record ChatMemorySnapshotRecord(
        GroupMemory groupMemory,
        List<LongTermMemoryData> longTermMemory,
        List<RecentMessage> recentMessages,
        List<MergedWebQAItem> vectorDbDocuments,
        RecentMessage lastAssistantMsg
) {
}