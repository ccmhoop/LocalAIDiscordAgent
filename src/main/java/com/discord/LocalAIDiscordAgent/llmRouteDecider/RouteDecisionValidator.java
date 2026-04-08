package com.discord.LocalAIDiscordAgent.llmRouteDecider;

import com.discord.LocalAIDiscordAgent.llmRouteDecider.records.RouteDecision;
import com.discord.LocalAIDiscordAgent.llmRouteDecider.records.RouteDecision.Mode;
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
                    reason == null ? "Defaulted to TEXT" : reason
            );
            case IMAGE -> new RouteDecision(
                    Mode.IMAGE,
                    normalizedPrompt == null ? "" : normalizedPrompt,
                    reason == null ? "Image request detected" : reason
            );
            case VIDEO -> new RouteDecision(
                    Mode.VIDEO,
                    normalizedPrompt == null ? "" : normalizedPrompt,
                    reason == null ? "Video request detected" : reason
            );
            case MUSIC -> new RouteDecision(
                    Mode.MUSIC,
                    normalizedPrompt == null ? "" : normalizedPrompt,
                    reason == null ? "music request detected" : reason
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