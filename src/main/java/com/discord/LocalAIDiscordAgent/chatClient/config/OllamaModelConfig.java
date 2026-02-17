package com.discord.LocalAIDiscordAgent.chatClient.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaModelConfig {

    @Bean
    public OllamaChatModel ollamaQwenModelConfig(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3:30b-thinking")
                                .enableThinking()
                                .temperature(1.5)
                                .numCtx(32768)      // context window (input+output+history)
                                .numPredict(4096)   // max generated tokens
                                .build()
                )
                .build();
    }
}
