package com.discord.LocalAIDiscordAgent.llm.llmRouteDecider;

import com.discord.LocalAIDiscordAgent.llm.llmRouteDecider.records.RouteDecision;
import com.discord.LocalAIDiscordAgent.llm.llmRouteDecider.records.RouteDecision.Mode;
import org.springframework.stereotype.Component;

@Component
public class RouteDecisionValidator {

    public RouteDecision normalize(RouteDecision decision) {
        if (decision == null || decision.mode() == null) {
            return RouteDecision.textFallback("Null or invalid route decision");
        }

        String normalizedPrompt = normalizeString(decision.normalizedPrompt());
        String reason = normalizeString(decision.reason());

        return switch (decision.mode()) {
            case TEXT -> new RouteDecision(
                    Mode.TEXT,
                    "",
                    reason == null ? "Defaulted to TEXT" : reason,
                    decision.requiresContext()
            );
            case IMAGE -> new RouteDecision(
                    Mode.IMAGE,
                    normalizedPrompt == null ? "" : normalizedPrompt,
                    reason == null ? "Image request detected" : reason,
                    decision.requiresContext()
            );
            case VIDEO -> new RouteDecision(
                    Mode.VIDEO,
                    normalizedPrompt == null ? "" : normalizedPrompt,
                    reason == null ? "Video request detected" : reason,
                    decision.requiresContext()
            );
            case MUSIC -> new RouteDecision(
                    Mode.MUSIC,
                    normalizedPrompt == null ? "" : normalizedPrompt,
                    reason == null ? "music request detected" : reason,
                    decision.requiresContext()
            );
        };
    }

    public boolean isUsable(RouteDecision decision) {
        if (decision == null || decision.mode() == null) {
            return false;
        }

        return switch (decision.mode()) {
            case TEXT -> true;
            case IMAGE, VIDEO, MUSIC-> decision.normalizedPrompt() != null && !decision.normalizedPrompt().isBlank();
        };
    }

    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}