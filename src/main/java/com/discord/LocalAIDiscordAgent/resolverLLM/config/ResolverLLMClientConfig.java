package com.discord.LocalAIDiscordAgent.resolverLLM.config;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResolverLLMClientConfig {

    @Bean
    public ChatClient resolverLLMClient(OllamaChatModel resolverLLMModel) {
        return ChatClient.builder(resolverLLMModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }

}
