package com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.repository;


import com.discord.LocalAIDiscordAgent.chatMemory.interfaces.ChatMemoryINTF;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.model.RecentChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecentChatMemoryRepository extends JpaRepository<RecentChatMemory, Long> {

//    List<T> findAllByUsername(String username);

}
