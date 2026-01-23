package com.discord.LocalAIDiscordAgent.aiOllama.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;

@Configuration
public class OllamaModelConfig {

    @Bean
    public OllamaChatModel ollamaQwenModelConfig(OllamaApi ollamaBasicApiConfig) {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(5)
                .exponentialBackoff(100, 2, 10000)
                .build();

        return OllamaChatModel.builder()
                .ollamaApi(ollamaBasicApiConfig)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3:4b")
                                .temperature(0.6)
                                .numPredict(1536)
                                .build())
                .retryTemplate(retryTemplate)
                .build();
    }

}
