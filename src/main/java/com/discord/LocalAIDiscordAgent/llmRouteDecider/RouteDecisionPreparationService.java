package com.discord.LocalAIDiscordAgent.llmRouteDecider;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmRouteDecider.records.RouteDecision;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RouteDecisionPreparationService {

    private final RouteDecisionService routeDecisionService;
    private final RouteDecisionValidator validator;
    private final MapperUtils mapperUtils;

    public RouteDecisionPreparationService(
            RouteDecisionService routeDecisionService,
            RouteDecisionValidator validator,
            MapperUtils mapperUtils
    ) {
        this.routeDecisionService = routeDecisionService;
        this.validator = validator;
        this.mapperUtils = mapperUtils;
    }

    public RouteDecision prepare(DiscGlobalData discGlobalData) {
//        PromptData promptData = new PromptData(mapperUtils);

        String normalizedUserMessage = normalize(discGlobalData.getUserMessage());
        if (normalizedUserMessage == null) {
//            promptData.setRouteDecision(RouteDecision.textFallback("Empty user message"));
//            return
        }

        RouteDecision rawDecision = routeDecisionService.decide(normalizedUserMessage);
        RouteDecision normalizedDecision = validator.normalize(rawDecision);

        log.info("Normalized route decision: {}", normalizedDecision);

        if (!validator.isUsable(normalizedDecision)) {
            normalizedDecision = RouteDecision.textFallback("Route decision failed validation");
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