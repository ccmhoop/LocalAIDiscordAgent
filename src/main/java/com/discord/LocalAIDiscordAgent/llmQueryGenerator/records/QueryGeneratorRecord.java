package com.discord.LocalAIDiscordAgent.llmQueryGenerator.records;

import java.util.List;

public record QueryGeneratorRecord(
        String systemMsg,
        List<String> instructions,
        QueryContextRecord context
) {
}
