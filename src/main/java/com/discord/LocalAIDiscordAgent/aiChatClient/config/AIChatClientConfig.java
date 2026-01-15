package com.discord.LocalAIDiscordAgent.aiChatClient.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AIChatClientConfig {

    @Bean
    public ChatClient chatClientOllamaKier(OllamaChatModel ollamaGeminiModelConfig, List<Advisor> scottishAdvisorsList) {
        return ChatClient.builder(ollamaGeminiModelConfig)
                .defaultAdvisors(scottishAdvisorsList)
                .build();
    }

}
