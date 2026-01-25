package com.discord.LocalAIDiscordAgent.aiTools.config;

import com.discord.LocalAIDiscordAgent.aiTools.websearch.AISearchEngineTool;
import com.discord.LocalAIDiscordAgent.aiTools.websearch.AIWebFilterTool;
import com.discord.LocalAIDiscordAgent.aiTools.websearch.AIWebSearchTool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AIToolConfig {

    @Bean("webSearch")
    public AIWebSearchTool webSearchTool() {
        return new AIWebSearchTool();
    }

    @Bean("webSearchEngine")
    public AISearchEngineTool webSearchEngineTool() {
        return new AISearchEngineTool();
    }

    @Bean("webFilterText")
    public AIWebFilterTool webFilterTool() {
        return new AIWebFilterTool();
    }

    @Bean
    public Object[] webSearchToolScottish(
            @Qualifier("webSearch") AIWebSearchTool webSearch,
            @Qualifier("webFilterText") AIWebFilterTool webFilterText,
            @Qualifier("webSearchEngine") AISearchEngineTool webSearchEngine) {
        return new Object[]{
                webSearch,
                webSearchEngine,
//                webFilterText
        };
    }

}
