package com.discord.LocalAIDiscordAgent.toolClient.config;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolClientConfig {

    @Bean
    public ChatClient executeToolsClient(OllamaChatModel executeToolsModel) {
        return ChatClient.builder(executeToolsModel)
                .build();
    }

    @Bean
    public ChatClient structuredToolClient(OllamaChatModel summerizeToolModel) {
        return ChatClient.builder(summerizeToolModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }

}
