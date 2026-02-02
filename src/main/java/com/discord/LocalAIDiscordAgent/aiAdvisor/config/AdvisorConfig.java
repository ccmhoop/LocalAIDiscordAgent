package com.discord.LocalAIDiscordAgent.aiAdvisor.config;


import com.discord.LocalAIDiscordAgent.aiAdvisor.advisors.ScottishAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AdvisorConfig {

    @Bean
    @Qualifier("scottishAdvisors")
    public List<Advisor> scottishAdvisors(
            VectorStore vectorStoreChatMemory,
            ChatMemory scottishChatMemoryConfig,
            VectorStore vectorStoreWebSearchMemory
    ) {
        return ScottishAdvisor.scottishAdvisorsList(
                vectorStoreChatMemory,
                scottishChatMemoryConfig,
                vectorStoreWebSearchMemory
        );
    }

    @Bean
    @Qualifier("scottishToolAdvisors")
    public List<Advisor> scottishToolAdvisors(
            ChatMemory scottishChatMemoryConfig,
            VectorStore vectorStoreWebSearchMemory
    ) {
        return ScottishAdvisor.toolAdvisor(
                scottishChatMemoryConfig,
                vectorStoreWebSearchMemory
        );
    }
}
