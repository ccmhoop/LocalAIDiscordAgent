package com.discord.LocalAIDiscordAgent.llmClients.structuredClient;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StructuredLLMModelConfig {

    /*
         Keep alive is set to 0s to prevent Ollama from filling the VRAM.
         This prevents freezing the file generation process on low VRAM devices.
     */
    @Bean
    public OllamaChatModel structuredLLMModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
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
}
