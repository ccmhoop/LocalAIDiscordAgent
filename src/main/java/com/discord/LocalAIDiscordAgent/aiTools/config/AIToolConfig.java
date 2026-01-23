package com.discord.LocalAIDiscordAgent.aiTools.config;

import com.discord.LocalAIDiscordAgent.aiTools.websearch.AIWebFilterTool;
import com.discord.LocalAIDiscordAgent.aiTools.websearch.AIWebSearchTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AIToolConfig {

    @Bean
    public List<Object> aiToolsConfig(){
        return List.of(new AIWebSearchTool(), new AIWebFilterTool());
    }


}
