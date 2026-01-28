package com.discord.LocalAIDiscordAgent.aiChatClient.config;

import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.systemMsg.ToolSystemMsg;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AIChatClientConfig {

    @Bean
    @Qualifier("scottishChatClient")
    public ChatClient scottishChatClient(
            OllamaChatModel ollamaQwenModelConfig,
            List<Advisor> scottishAdvisorsList
    ) {
        return ChatClient.builder(ollamaQwenModelConfig)
                .defaultAdvisors(scottishAdvisorsList)
                .build();
    }

    @Bean
    @Qualifier("scottishToolChatClient")
    public ChatClient scottishToolChatClient(
            OllamaChatModel ollamaQwenModelConfig,
            Object[] webSearchToolScottish
    ) {
        return ChatClient.builder(ollamaQwenModelConfig)
                .defaultSystem(ToolSystemMsg.WEB_SEARCH_TOOL_INSTRUCTIONS)
                .defaultTools(webSearchToolScottish)
                .build();
    }
}