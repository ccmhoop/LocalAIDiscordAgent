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
            OllamaChatModel qwenChatModel,
            SystemMessageFactory systemMessageFactory
    ) {
        return ChatClient.builder(qwenChatModel)
                .defaultSystem(systemMessageFactory.buildDefaultSystemMessage())
                .build();
    }

    @Bean
    public ChatClient summaryChatClient(OllamaChatModel qwenChatSummaryModel) {
        return ChatClient.builder(qwenChatSummaryModel)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }

}
