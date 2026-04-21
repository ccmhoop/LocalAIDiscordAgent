package com.discord.LocalAIDiscordAgent.llm.config.model;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfigModel {

    @Bean
    public OllamaChatModel llmTextModel(OllamaApi ollamaBasicApiConfig) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaBasicApiConfig)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("lfm2")
                                .keepAlive("0s")
                                .disableThinking()
                                .temperature(0.5)
                                .topP(0.92)
                                .repeatPenalty(1.15)
                                .numCtx(32768)
                                .numPredict(4096)
                                .build()
                )
                .build();
    }


    @Bean
    public OllamaChatModel llmStructuredModel(OllamaApi ollamaBasicApiConfig) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaBasicApiConfig)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3.5:9b")
                                .keepAlive("0s")
                                .disableThinking()
                                .temperature(0.7)
                                .numCtx(8192)
                                .numPredict(512)
                                .build()
                )
                .build();
    }


    @Bean
    public OllamaChatModel llmToolModel(OllamaApi ollamaBasicApiConfig) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaBasicApiConfig)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3.5:27b")
                                .enableThinking()
                                .temperature(0.5)
                                .topP(0.92)
                                .keepAlive("0s")
                                .repeatPenalty(1.15)
                                .numCtx(32768)
                                .numPredict(4096)
                                .build()
                )
                .build();
    }

    @Bean
    public OllamaChatModel llmPayloadModel(OllamaApi ollamaBasicApiConfig) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaBasicApiConfig)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("ministral-3:14b")
                                .numCtx(4096)
                                .numPredict(1200)
                                .disableThinking()
                                .temperature(0.2)
                                .keepAlive("0s")
                                .build()
                )
                .build();
    }
}
