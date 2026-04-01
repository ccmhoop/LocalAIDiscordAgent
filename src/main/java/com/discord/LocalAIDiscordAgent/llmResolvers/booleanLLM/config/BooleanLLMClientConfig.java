package com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.config;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BooleanLLMClientConfig {

    @Bean
    public ChatClient booleanLLMClient(OllamaChatModel booleanLLMModel) {
        return ChatClient.builder(booleanLLMModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }

}
