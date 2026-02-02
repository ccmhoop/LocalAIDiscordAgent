package com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.config;

import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.tools.DirectLinkTool;
import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.tools.WebSearchTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration()
public class AIWebToolConfig {

    @Bean
    @Qualifier("webSearchToolScottish")
    public Object[] webSearchToolScottish(
            WebSearchTool webSearchTool,
            DirectLinkTool directLinkTool
    ) {
        return new Object[]{
                directLinkTool,
                webSearchTool
        };
    }
}
