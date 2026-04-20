package com.discord.LocalAIDiscordAgent.llmClients.chatClient.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfig {

    @Bean
    public OllamaChatModel qwenChatModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaChatOptions.builder()
//                                .model("qwen3.5:27b")
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
    public OllamaChatModel qwenChatSummaryModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3.5:9b")
                                .keepAlive("0s")
                                .disableThinking()
                                .temperature(0.0)
                                .numCtx(32768)
                                .numPredict(4096)
                                .build()
                )
                .build();
    }

}
