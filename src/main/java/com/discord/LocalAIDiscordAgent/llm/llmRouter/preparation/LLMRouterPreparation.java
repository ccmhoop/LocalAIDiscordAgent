package com.discord.LocalAIDiscordAgent.llm.llmRouter.preparation;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmRouter.llmCall.LLMRouterCall;
import com.discord.LocalAIDiscordAgent.llm.llmRouter.dto.LLMRouterDTO;
import com.discord.LocalAIDiscordAgent.llm.llmRouter.validation.LLMRouterValidation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LLMRouterPreparation {

    private final LLMRouterCall llmRouterCall;
    private final LLMRouterValidation validator;

    public LLMRouterPreparation(
            LLMRouterCall llmRouterCall,
            LLMRouterValidation validator
    ) {
        this.llmRouterCall = llmRouterCall;
        this.validator = validator;
    }

    public LLMRouterDTO prepare(DiscGlobalData discGlobalData) {

        String normalizedUserMessage = normalize(discGlobalData.getUserMessage());
        if (normalizedUserMessage == null) {
//            promptData.setRouteDecision(RouteDecision.textFallback("Empty user message"));
//            return
        }

        LLMRouterDTO rawDecision = llmRouterCall.decide(normalizedUserMessage);
        LLMRouterDTO normalizedDecision = validator.normalize(rawDecision);


        if (!validator.isUsable(normalizedDecision)) {
            normalizedDecision = LLMRouterDTO.textFallback("Route decision failed validation");
        }

//        promptData.setRouteDecision(normalizedDecision);
        return normalizedDecision;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}