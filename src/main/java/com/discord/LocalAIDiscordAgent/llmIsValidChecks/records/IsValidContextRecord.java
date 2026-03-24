package com.discord.LocalAIDiscordAgent.llmIsValidChecks.records;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;

import java.util.List;

public record IsValidContextRecord(
        GroupMemory groupMemory,
        List<LongTermMemoryData> longTermMemory,
        List<RecentMessage> recentMessages,
        List<MergedWebQAItem> vectorDbDocuments,
        RecentMessage lastAssistantMsg
) {
}