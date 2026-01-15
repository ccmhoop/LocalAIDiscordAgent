package com.discord.LocalAIDiscordAgent.aiOllama.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaModelConfig {

    @Bean
    public OllamaChatModel ollamaGeminiModelConfig(OllamaApi ollamaBasicApiConfig) {
        return  OllamaChatModel.builder()
                .ollamaApi(ollamaBasicApiConfig)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("gemma3:27b")
                                .temperature(0.9)
                                .numPredict(384)
                                .build())
                .build();
    }

}
