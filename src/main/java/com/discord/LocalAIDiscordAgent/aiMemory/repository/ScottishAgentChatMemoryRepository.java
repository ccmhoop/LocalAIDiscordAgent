package com.discord.LocalAIDiscordAgent.aiMemory.repository;

import lombok.NonNull;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;

public class ScottishAgentChatMemoryRepository implements JdbcChatMemoryRepositoryDialect {

    @Override
    @NonNull
    public String getSelectMessagesSql() {
        return "SELECT content, type FROM SCOTTISH_AI_CHAT_MEMORY WHERE conversation_id = ? ORDER BY \"timestamp\"";
    }

    @Override
    @NonNull
    public String getInsertMessageSql() {
        return "INSERT INTO SCOTTISH_AI_CHAT_MEMORY  (conversation_id, content, type, \"timestamp\") VALUES (?, ?, ?, ?)";
    }

    @Override
    @NonNull
    public String getSelectConversationIdsSql() {
        return "SELECT DISTINCT conversation_id FROM SCOTTISH_AI_CHAT_MEMORY ";
    }

    @Override
    @NonNull
    public String getDeleteMessagesSql() {
        return "DELETE FROM SCOTTISH_AI_CHAT_MEMORY  WHERE conversation_id = ?";
    }

}
