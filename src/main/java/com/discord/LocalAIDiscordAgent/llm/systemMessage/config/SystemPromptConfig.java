package com.discord.LocalAIDiscordAgent.llm.systemMessage.config;

import com.discord.LocalAIDiscordAgent.llm.systemMessage.SystemMessageFactory;
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

}