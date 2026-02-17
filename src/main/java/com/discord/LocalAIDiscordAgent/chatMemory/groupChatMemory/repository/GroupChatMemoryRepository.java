package com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.repository;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.model.GroupChatMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface GroupChatMemoryRepository extends JpaRepository<GroupChatMemory, Long> {

    void deleteAllByUsername(String username);
    void deleteByTimestampBefore(LocalDateTime timestamp);

}
