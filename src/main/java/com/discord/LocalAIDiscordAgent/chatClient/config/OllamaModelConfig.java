package com.discord.LocalAIDiscordAgent.chatClient.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaModelConfig {

    @Bean
    public OllamaChatModel ollamaQwenModelConfig(OllamaApi ollamaBasicApiConfig) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaBasicApiConfig)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3:30b")
                                .temperature(0.6)
                                .numPredict(1536)
                                .build())
                .build();
    }

}
