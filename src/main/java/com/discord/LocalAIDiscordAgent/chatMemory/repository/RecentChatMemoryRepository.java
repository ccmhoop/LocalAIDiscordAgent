package com.discord.LocalAIDiscordAgent.chatMemory.repository;


import com.discord.LocalAIDiscordAgent.chatMemory.model.RecentChatMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecentChatMemoryRepository extends JpaRepository<RecentChatMemory, Long> {

    List<RecentChatMemory> findAllByUsername(String username);

}
