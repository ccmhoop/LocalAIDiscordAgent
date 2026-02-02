package com.discord.LocalAIDiscordAgent.chatClient.config;

import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.systemMsg.ToolSystemMsg;
import com.discord.LocalAIDiscordAgent.chatClient.systemMsg.AISystemMsg;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MainChatClientConfig {

    @Bean
    @Qualifier("scottishChatClient")
    public ChatClient scottishChatClient(
            OllamaChatModel ollamaQwenModelConfig,
            @Qualifier("scottishAdvisors") List<Advisor> scottishAdvisorConfig
    ) {
        return ChatClient.builder(ollamaQwenModelConfig)
                .defaultSystem(AISystemMsg.SYSTEM_MESSAGE_SCOTTISH_AGENT)
                .defaultAdvisors(scottishAdvisorConfig)
                .build();
    }

    @Bean
    @Qualifier("scottishToolChatClient")
    public ChatClient scottishToolChatClient(
            OllamaChatModel ollamaQwenModelConfig,
            @Qualifier("scottishToolAdvisors") List<Advisor> scottishToolAdvisorConfig,
            Object[] webSearchToolScottish
    ) {
        return ChatClient.builder(ollamaQwenModelConfig)
                .defaultSystem(
                        AISystemMsg.SYSTEM_MESSAGE_SCOTTISH_AGENT
                                + ToolSystemMsg.WEB_SEARCH_TOOL_INSTRUCTIONS
                )
                .defaultAdvisors(scottishToolAdvisorConfig)
                .defaultTools(webSearchToolScottish)
                .build();
    }
}
