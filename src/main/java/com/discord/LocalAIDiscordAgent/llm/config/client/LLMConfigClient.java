package com.discord.LocalAIDiscordAgent.llm.config.client;

import com.discord.LocalAIDiscordAgent.llm.systemMessage.SystemMessageFactory;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfigClient {

    @Bean
    public ChatClient llmTextClient(
            OllamaChatModel llmTextModel,
            SystemMessageFactory systemMessageFactory
    ) {
        return ChatClient.builder(llmTextModel)
                .defaultSystem(systemMessageFactory.buildDefaultSystemMessage())
                .build();
    }

    @Bean
    public ChatClient llmStructuredClient(OllamaChatModel llmStructuredModel) {
        return ChatClient.builder(llmStructuredModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }

    @Bean
    public ChatClient llmToolClient(OllamaChatModel llmToolModel) {
        return ChatClient.builder(llmToolModel)
                .build();
    }

}
