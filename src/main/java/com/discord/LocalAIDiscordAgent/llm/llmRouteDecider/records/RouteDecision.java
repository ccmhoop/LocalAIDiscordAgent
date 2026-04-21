package com.discord.LocalAIDiscordAgent.llm.llmRouteDecider.records;

public record RouteDecision(
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
    public static RouteDecision textFallback(String reason) {
        return new RouteDecision(Mode.TEXT, "", reason, false);
    }
}