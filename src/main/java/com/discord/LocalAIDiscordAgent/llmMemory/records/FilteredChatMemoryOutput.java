package com.discord.LocalAIDiscordAgent.llmMemory.records;


import java.util.List;

public record FilteredChatMemoryOutput(
        boolean includeLongTermMemory,
        List<Integer> ids
) { }
