package com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.config;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterLLMClientConfig {

    @Bean
    public ChatClient filterLLMClient(OllamaChatModel filterLLMModel) {
        return ChatClient.builder(filterLLMModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }

}
