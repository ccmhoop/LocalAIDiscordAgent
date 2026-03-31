package com.discord.LocalAIDiscordAgent.structuredLLM.records;

import java.util.List;

public record StructuredLLMPayloadRecord(
        String systemMsg,
        List<String> instructions,
        StructuredLLMContextRecord context
) {
}
