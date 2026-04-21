package com.discord.LocalAIDiscordAgent.memory.chatMemory.records;

import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.memory.chatMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.records.WebSearchRecords.MergedWebQAItem;

import java.util.List;

public record ChatMemorySnapshotRecord(
        GroupMemory groupMemory,
        List<LongTermMemoryData> longTermMemory,
        List<RecentMessage> recentMessages,
        List<MergedWebQAItem> vectorDbDocuments,
        RecentMessage lastAssistantMsg
) {
}