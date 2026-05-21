package com.discord.LocalAIDiscordAgent.llm.llmRouter.dto;

public record LLMRouterDTO(
        Mode mode,
        String normalizedPrompt,
        String reason,
        Boolean requiresContext
) {
    public enum Mode {
        TEXT,
        IMAGE,
        VIDEO,
        MUSIC
    }
    public static LLMRouterDTO textFallback(String reason) {
        return new LLMRouterDTO(Mode.TEXT, "", reason, false);
    }
}