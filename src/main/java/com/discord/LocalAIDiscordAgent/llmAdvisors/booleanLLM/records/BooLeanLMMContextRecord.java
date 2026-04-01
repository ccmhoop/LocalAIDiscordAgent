package com.discord.LocalAIDiscordAgent.llmAdvisors.booleanLLM.records;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.llmMemory.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;

import java.util.List;

public record BooLeanLMMContextRecord(
        GroupMemory groupMemory,
        List<LongTermMemoryData> longTermMemory,
        List<RecentMessage> recentMessages,
        List<MergedWebQAItem> vectorDbDocuments,
        RecentMessage lastAssistantMsg
) {
}