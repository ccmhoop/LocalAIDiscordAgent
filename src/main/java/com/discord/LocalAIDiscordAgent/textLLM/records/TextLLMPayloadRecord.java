package com.discord.LocalAIDiscordAgent.textLLM.records;

import java.util.List;

public record TextLLMPayloadRecord(
        String systemMsg,
        List<String> instructions,
        TextLLMContextRecord context
) {
}
