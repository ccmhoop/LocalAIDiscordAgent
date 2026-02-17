package com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.repository;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.model.GroupChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.interfaces.ChatMemoryINTF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupChatMemoryRepository extends JpaRepository<GroupChatMemory, Long> {

    void deleteAllByUsername(String username);

    void deleteByTimestampBefore(LocalDateTime timestamp);

}
