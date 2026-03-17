package com.discord.LocalAIDiscordAgent.systemMessage.config;

import com.discord.LocalAIDiscordAgent.systemMessage.SystemMessageFactory;
import com.discord.LocalAIDiscordAgent.systemMessage.ToolSystemMsgFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemPromptConfig {

    @Bean
    public SystemMessageFactory systemMessageFactory(
            @Qualifier("aiObjectMapper") ObjectMapper aiObjectMapper
    ) {
        return new SystemMessageFactory(aiObjectMapper);
    }

    @Bean
    public ToolSystemMsgFactory toolSystemMsgFactory(
            @Qualifier("aiObjectMapper") ObjectMapper aiObjectMapper
    ) {
        return new ToolSystemMsgFactory(aiObjectMapper);
    }
}