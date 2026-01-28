package com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.config;

import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.service.WebSearchMemoryService;
import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.tools.AISearchEngineTool;
import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.tools.AIWebSearchTool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration(proxyBeanMethods = false)
public class AIWebToolConfig {

    @Bean("webSearchEngine")
    public AISearchEngineTool webSearchEngineTool(WebSearchMemoryService webSearchMemoryService) {
        return new AISearchEngineTool(webSearchMemoryService);
    }

    @Bean("webSearch")
    public AIWebSearchTool webSearchTool(
            WebSearchMemoryService webSearchMemoryService,
            @Qualifier("webSearchEngine") AISearchEngineTool webSearchEngine
    ) {
        return new AIWebSearchTool(webSearchMemoryService, Optional.of(webSearchEngine));
    }

    @Bean
    public Object[] webSearchToolScottish(
            @Qualifier("webSearch") AIWebSearchTool webSearch,
            @Qualifier("webSearchEngine") AISearchEngineTool webSearchEngine
    ) {
        return new Object[]{
                webSearch,
                webSearchEngine
        };
    }
}
