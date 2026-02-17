package com.discord.LocalAIDiscordAgent.chatMemory.toolChatMemory.config;

import com.discord.LocalAIDiscordAgent.chatMemory.toolChatMemory.encoder.EncodingChatMemoryRepository;
import com.discord.LocalAIDiscordAgent.chatMemory.toolChatMemory.jdbcDialects.ToolMemoryDialect;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class MemoryConfig {

    @Bean
    public ChatMemory webMemory(JdbcTemplate jdbcTemplate) {

        ChatMemoryRepository baseRepo = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .dialect(new ToolMemoryDialect())
                .build();

        ChatMemoryRepository encodingRepo = new EncodingChatMemoryRepository(
                baseRepo);

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(encodingRepo)
                .maxMessages(10)
                .build();
    }
}




