package com.discord.LocalAIDiscordAgent.chatClient.config;

import com.discord.LocalAIDiscordAgent.systemMessage.SystemMessageFactory;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient advisorChatClient(
            OllamaChatModel ollamaQwenModelConfig,
            SystemMessageFactory systemMessageFactory
    ) {
        String systemMessage = systemMessageFactory.buildDefaultSystemMessage();
        return ChatClient.builder(ollamaQwenModelConfig)
                .defaultSystem(systemMessage)
                .build();
    }

    @Bean
    public ChatClient summaryChatClient(OllamaChatModel ollamaQwenSummaryConfig) {
        return ChatClient.builder(ollamaQwenSummaryConfig)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }

    @Bean
    public ChatClient toolCallingLLM(OllamaChatModel llmToolThinkingConfig) {
        return ChatClient.builder(llmToolThinkingConfig)
                .build();
    }

    @Bean
    public ChatClient toolClientLLM(OllamaChatModel llmToolConfig) {
        return ChatClient.builder(llmToolConfig)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }


}
