package com.discord.LocalAIDiscordAgent.llmRouteDecider.records;

public record RouteDecision(
        Mode mode,
        String normalizedPrompt,
        String reason
) {
    public enum Mode {
        TEXT,
        IMAGE,
        VIDEO
    }
    public static RouteDecision textFallback(String reason) {
        return new RouteDecision(Mode.TEXT, "", reason);
    }
}