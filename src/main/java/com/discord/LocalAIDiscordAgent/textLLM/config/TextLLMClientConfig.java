package com.discord.LocalAIDiscordAgent.textLLM.config;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TextLLMClientConfig {

    @Bean
    public ChatClient textLLMClient(OllamaChatModel textLLMModel) {
        return ChatClient.builder(textLLMModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }
}
