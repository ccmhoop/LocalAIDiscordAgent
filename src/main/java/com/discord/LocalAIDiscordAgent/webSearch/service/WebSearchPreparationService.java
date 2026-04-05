package com.discord.LocalAIDiscordAgent.webSearch.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WebSearchPreparationService {

    private final WebSearchNecessityService necessityService;
    private final WebSearchRuleEngine ruleEngine;
    private final MapperUtils mapperUtils;

    public WebSearchPreparationService(
            WebSearchNecessityService necessityService,
            WebSearchRuleEngine ruleEngine,
            MapperUtils mapperUtils
    ) {
        this.necessityService = necessityService;
        this.ruleEngine = ruleEngine;
        this.mapperUtils = mapperUtils;
    }

    public void prepare(DiscGlobalData discGlobalData, PromptData promptData) {
//        PromptData promptData = new PromptData(mapperUtils);

//        String normalizedUserMessage = normalize(userMessage);
//        if (normalizedUserMessage == null) {
//            return promptData;
//        }
        String normalizedUserMessage = discGlobalData.getUserMessage();
        boolean forcedSearch = ruleEngine.shouldForceSearch(normalizedUserMessage);
        if (forcedSearch) {
            promptData.setWebSearchRequired(true);
//            return promptData;
        }

        boolean softSignal = necessityService.needsWebSearch(
                normalizedUserMessage,
                normalize("")
        );

        log.info("Web-search soft signal: {}", softSignal);

        boolean hardDecision = ruleEngine.isUsableDecision(normalizedUserMessage, softSignal);
        log.info("Web-search hard decision: {}", hardDecision);

        promptData.setWebSearchRequired(hardDecision);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}