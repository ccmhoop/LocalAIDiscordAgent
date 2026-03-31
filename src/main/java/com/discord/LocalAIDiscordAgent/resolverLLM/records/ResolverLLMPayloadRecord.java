package com.discord.LocalAIDiscordAgent.resolverLLM.records;

public record ResolverLLMPayloadRecord(
        String systemMsg,
        ResolverLMMContextRecord payloadContext
) {
}
