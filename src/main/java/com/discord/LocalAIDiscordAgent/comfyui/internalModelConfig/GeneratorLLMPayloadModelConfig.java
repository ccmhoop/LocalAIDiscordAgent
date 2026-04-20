package com.discord.LocalAIDiscordAgent.comfyui.internalModelConfig;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeneratorLLMPayloadModelConfig {

    @Bean
    public OllamaChatModel generatorPayloadChatModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
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
