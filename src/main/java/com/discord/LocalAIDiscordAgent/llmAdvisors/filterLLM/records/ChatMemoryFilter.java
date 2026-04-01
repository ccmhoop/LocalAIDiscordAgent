package com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.records;

import java.util.List;

public record ChatMemoryFilter(
        String systemMsg,
        List<String> instruction,
        FilterLLMContextRecord chatMemoryContext
) {
}
