package com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.config;

import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.tools.AISearchEngineTool;
import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.tools.AIWebSearchTool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIWebToolConfig {

    @Bean("webSearch")
    public AIWebSearchTool webSearchTool() {
        return new AIWebSearchTool();
    }

    @Bean("webSearchEngine")
    public AISearchEngineTool webSearchEngineTool() {
        return new AISearchEngineTool();
    }


    @Bean
    public Object[] webSearchToolScottish(
            @Qualifier("webSearch") AIWebSearchTool webSearch,
            @Qualifier("webSearchEngine") AISearchEngineTool webSearchEngine
    ) {
        return new Object[]{
                webSearch,
                webSearchEngine,
        };
    }


}
