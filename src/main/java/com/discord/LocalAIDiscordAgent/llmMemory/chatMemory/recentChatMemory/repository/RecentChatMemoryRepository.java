package com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.recentChatMemory.repository;


import com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.recentChatMemory.model.RecentChatMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecentChatMemoryRepository extends JpaRepository<RecentChatMemory, Long> {
    List<RecentChatMemory> findAllByConversationId(String conversationId);
}
