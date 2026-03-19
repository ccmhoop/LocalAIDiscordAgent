package com.discord.LocalAIDiscordAgent.ollama;

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
                                .model("qwen3.5:27b")
//                                .enableThinking()
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
    public OllamaChatModel ollamaQwenSummaryConfig(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3.5:9b")
                                .disableThinking()      // summarizer should be deterministic + fast :contentReference[oaicite:5]{index=5}
                                .temperature(0.0)       // deterministic :contentReference[oaicite:6]{index=6}
//                                .seed(42)               // repeatable outputs :contentReference[oaicite:7]{index=7}
//                                .topK(40)               // default is fine :contentReference[oaicite:8]{index=8}
//                                .topP(0.9)              // default is fine :contentReference[oaicite:9]{index=9}
//                                .repeatPenalty(1.1)     // default is fine :contentReference[oaicite:10]{index=10}
                                .numCtx(32768)           // or 16384 if you sometimes inject lots of text :contentReference[oaicite:11]{index=11}
                                .numPredict(4096)        // plenty for JSON summary+facts :contentReference[oaicite:12]{index=12}
                                .build()
                )
                .build();
    }

    @Bean
    public OllamaChatModel summerizeToolModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3.5:9b")
                                .disableThinking()
                                .temperature(0.0)
                                .numCtx(32768)
                                .numPredict(4096)
                                .build()
                )
                .build();
    }

    @Bean
    public OllamaChatModel executeToolsModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3.5:9b")
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
    public OllamaChatModel queryGeneratorToolModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .model("qwen3.5:9b")
                                .disableThinking()
                                .temperature(0.0)
                                .topP(0.92)
                                .repeatPenalty(1.15)
                                .numCtx(32768)
                                .numPredict(4096)
                                .build()
                )
                .build();
    }
}
