package com.discord.LocalAIDiscordAgent.vectorStoreEmbedding.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingModelConfig {

    @Bean
    public EmbeddingModel embeddingModel(OllamaApi ollamaBasicApiConfig) {
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaBasicApiConfig)
                .defaultOptions(
                        OllamaEmbeddingOptions.builder()
                                .model("mxbai-embed-large")
                                .build())
                .build();
    }

}
