package com.discord.LocalAIDiscordAgent.chatClient.config;

import com.discord.LocalAIDiscordAgent.chatClient.systemMsg.SystemMsg;
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
            List<Advisor> agentChatAdvisors
    ) {
        return ChatClient.builder(ollamaQwenModelConfig)
                .defaultSystem(SystemMsg.SYSTEM_MESSAGE_AGENT)
                .defaultAdvisors(agentChatAdvisors)
                .build();
    }

}
