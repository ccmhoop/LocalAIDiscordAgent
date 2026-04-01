package com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.records;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.llmMemory.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;

import java.util.List;

public record StructuredLLMContextRecord(
        List<LongTermMemoryData> longTermMemory,
        RecentMessage lastAssistantMsg,
        List<MergedWebQAItem> vectorDBMemory,
        String Summary

) {
}
