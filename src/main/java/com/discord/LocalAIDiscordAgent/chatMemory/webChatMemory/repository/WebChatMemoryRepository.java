package com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.repository;

import com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.model.WebChatMemory;
import com.discord.LocalAIDiscordAgent.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebChatMemoryRepository extends JpaRepository<WebChatMemory, Long> {
    void deleteAllByUser(UserEntity user);
    List<WebChatMemory> findAllByUser(UserEntity user);
}
