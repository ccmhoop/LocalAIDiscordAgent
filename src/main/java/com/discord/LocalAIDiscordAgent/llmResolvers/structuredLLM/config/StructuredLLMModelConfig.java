package com.discord.LocalAIDiscordAgent.llmResolvers.structuredLLM.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StructuredLLMModelConfig {

    @Bean
    public OllamaChatModel structuredLLMModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3.5:9b")
//                                .model("lfm2")
//                                .model("devstral-small-2:24b")
                                .disableThinking()
                                .temperature(0.7)
                                .numCtx(8192)
                                .numPredict(512)
                                .build()
                )
                .build();
    }
}
