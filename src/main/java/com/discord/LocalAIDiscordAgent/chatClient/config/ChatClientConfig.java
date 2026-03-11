package com.discord.LocalAIDiscordAgent.chatClient.config;

import com.discord.LocalAIDiscordAgent.systemMessage.SystemMessageFactory;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient advisorChatClient(
            OllamaChatModel ollamaQwenModelConfig,
            List<Advisor> agentChatAdvisors,
            SystemMessageFactory systemMessageFactory
    ) {
        String systemMessage = systemMessageFactory.buildDefaultSystemMessage();

        System.out.println(systemMessage);

        return ChatClient.builder(ollamaQwenModelConfig)
                .defaultSystem(systemMessage)
//                .defaultAdvisors(agentChatAdvisors)
                .build();
    }

    @Bean
    public ChatClient summaryChatClient(OllamaChatModel ollamaQwenSummaryConfig) {
        return ChatClient.builder(ollamaQwenSummaryConfig)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }


}
