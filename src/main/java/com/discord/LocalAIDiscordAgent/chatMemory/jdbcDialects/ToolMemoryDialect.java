package com.discord.LocalAIDiscordAgent.chatMemory.jdbcDialects;

import lombok.NonNull;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;

public class ToolMemoryDialect  implements JdbcChatMemoryRepositoryDialect {

    @Override
    @NonNull
    public String getSelectMessagesSql() {
        return "SELECT content, type FROM TOOL_MEMORY WHERE conversation_id = ? ORDER BY \"timestamp\"";
    }

    @Override
    @NonNull
    public String getInsertMessageSql() {
        return "INSERT INTO TOOL_MEMORY  (conversation_id, content, type, \"timestamp\") VALUES (?, ?, ?, ?)";
    }

    @Override
    @NonNull
    public String getSelectConversationIdsSql() {
        return "SELECT DISTINCT conversation_id FROM TOOL_MEMORY ";
    }

    @Override
    @NonNull
    public String getDeleteMessagesSql() {
        return "DELETE FROM TOOL_MEMORY  WHERE conversation_id = ?";
    }

}
