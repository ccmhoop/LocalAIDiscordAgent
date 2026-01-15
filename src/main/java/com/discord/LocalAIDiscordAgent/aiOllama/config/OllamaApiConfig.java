package com.discord.LocalAIDiscordAgent.aiOllama.config;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaApiConfig {

    @Bean
    public OllamaApi ollamaBasicApiConfig() {
        return OllamaApi.builder().build();
    }

}
