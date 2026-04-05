package com.discord.LocalAIDiscordAgent.llmClients.chatSummary.repository;

import com.discord.LocalAIDiscordAgent.llmClients.chatSummary.model.ChatSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSummaryRepository extends JpaRepository<ChatSummary, String> {
}