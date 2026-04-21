package com.discord.LocalAIDiscordAgent.llm.llmTools.systemMessage.config;

import com.discord.LocalAIDiscordAgent.llm.llmTools.systemMessage.ToolSystemMsgFactory;
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
