package com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.service;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class WebSearchRuleEngine {

    public boolean shouldForceSearch(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }

        String text = userMessage.toLowerCase(Locale.ROOT);

        return containsAny(text,
                "search",
                "web search",
                "look up",
                "lookup",
                "google",
                "verify",
                "check the latest",
                "latest information",
                "find sources",
                "find citations",
                "find citation",
                "with sources",
                "with citations"
        );
    }

    public boolean isUsableDecision(String userMessage, boolean llmDecision) {
        if (shouldForceSearch(userMessage)) {
            return true;
        }
        return llmDecision;
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }
}