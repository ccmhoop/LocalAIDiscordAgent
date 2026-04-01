package com.discord.LocalAIDiscordAgent.llmMain.chatSummary.repository;

import com.discord.LocalAIDiscordAgent.llmMain.chatSummary.model.ChatSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSummaryRepository extends JpaRepository<ChatSummary, String> {
}