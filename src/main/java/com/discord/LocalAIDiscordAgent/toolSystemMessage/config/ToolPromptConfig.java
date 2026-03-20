package com.discord.LocalAIDiscordAgent.toolSystemMessage.config;

import com.discord.LocalAIDiscordAgent.toolSystemMessage.ToolSystemMsgFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolPromptConfig {
    @Bean
    public ToolSystemMsgFactory toolSystemMsgFactory(
            @Qualifier("aiObjectMapper") ObjectMapper aiObjectMapper
    ) {
        return new ToolSystemMsgFactory(aiObjectMapper);
    }
}
