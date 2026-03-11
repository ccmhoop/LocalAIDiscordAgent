package com.discord.LocalAIDiscordAgent.chatSummary.repository;

import com.discord.LocalAIDiscordAgent.chatSummary.model.ChatSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSummaryRepository extends JpaRepository<ChatSummary, String> {
}