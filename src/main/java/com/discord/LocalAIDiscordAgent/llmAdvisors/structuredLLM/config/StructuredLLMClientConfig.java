package com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.config;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StructuredLLMClientConfig {

    @Bean
    public ChatClient structuredLLMClient(OllamaChatModel structuredLLMModel) {
        return ChatClient.builder(structuredLLMModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }
}
