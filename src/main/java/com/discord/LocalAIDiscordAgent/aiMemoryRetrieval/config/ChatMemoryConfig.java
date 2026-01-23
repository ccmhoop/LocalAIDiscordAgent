package com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.config;

import com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.repository.ScottishAgentChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ChatMemoryConfig {

    private final JdbcTemplate jdbcTemplate;

    public ChatMemoryConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Bean
    public ChatMemory scottishChatMemoryConfig(){
        ChatMemoryRepository chatMemoryRepository = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .dialect(new ScottishAgentChatMemoryRepository())
                .build();

       return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();
    }

}
