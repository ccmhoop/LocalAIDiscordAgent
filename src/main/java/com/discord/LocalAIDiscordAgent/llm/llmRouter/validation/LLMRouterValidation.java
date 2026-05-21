package com.discord.LocalAIDiscordAgent.llm.llmRouter.validation;

import com.discord.LocalAIDiscordAgent.llm.llmRouter.dto.LLMRouterDTO;
import com.discord.LocalAIDiscordAgent.llm.llmRouter.dto.LLMRouterDTO.Mode;
import org.springframework.stereotype.Component;

@Component
public class LLMRouterValidation {

    public LLMRouterDTO normalize(LLMRouterDTO decision) {
        if (decision == null || decision.mode() == null) {
            return LLMRouterDTO.textFallback("Null or invalid route decision");
        }

        String normalizedPrompt = normalizeString(decision.normalizedPrompt());
        String reason = normalizeString(decision.reason());

        return switch (decision.mode()) {
            case TEXT -> new LLMRouterDTO(
                    Mode.TEXT,
                    "",
                    reason == null ? "Defaulted to TEXT" : reason,
                    decision.requiresContext()
            );
            case IMAGE -> new LLMRouterDTO(
                    Mode.IMAGE,
                    normalizedPrompt == null ? "" : normalizedPrompt,
                    reason == null ? "Image request detected" : reason,
                    decision.requiresContext()
            );
            case VIDEO -> new LLMRouterDTO(
                    Mode.VIDEO,
                    normalizedPrompt == null ? "" : normalizedPrompt,
                    reason == null ? "Video request detected" : reason,
                    decision.requiresContext()
            );
            case MUSIC -> new LLMRouterDTO(
                    Mode.MUSIC,
                    normalizedPrompt == null ? "" : normalizedPrompt,
                    reason == null ? "music request detected" : reason,
                    decision.requiresContext()
            );
        };
    }

    public boolean isUsable(LLMRouterDTO decision) {
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