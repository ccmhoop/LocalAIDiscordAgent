package com.discord.LocalAIDiscordAgent.memory.chatMemory.groupChatMemory.repository;

import com.discord.LocalAIDiscordAgent.memory.chatMemory.groupChatMemory.model.GroupChatMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupChatMemoryRepository extends JpaRepository<GroupChatMemory, Long> {

    List<GroupChatMemory> findAllByConversationIdOrderByTimestampAsc(String conversationId);

    List<GroupChatMemory> findAllByConversationIdAndTimestampBeforeOrderByTimestampAsc(
            String conversationId,
            LocalDateTime timestamp
    );

}
