package com.discord.LocalAIDiscordAgent.llmResolvers.filterLLM.records;

import java.util.List;

public record ChatMemoryFilter(
        String systemMsg,
        List<String> instruction,
        ChatMemoryContext chatMemoryContext
) {
}
