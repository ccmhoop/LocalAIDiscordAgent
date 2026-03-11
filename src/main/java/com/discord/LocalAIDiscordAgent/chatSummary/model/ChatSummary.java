package com.discord.LocalAIDiscordAgent.chatSummary.model;

import com.discord.LocalAIDiscordAgent.chatSummary.records.SummaryRecords.MemoryState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
public class ChatSummary {

    @Id
    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(name = "summary", nullable = false, columnDefinition = "text")
    private String summary = "";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "facts_json", nullable = false, columnDefinition = "jsonb")
    private String factsJson = "[]";

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "last_summarized_ts")
    private String lastSummarizedTs;

    protected ChatSummary() {}

    public ChatSummary(String conversationId) {
        this.conversationId = conversationId;
    }

    public void apply(MemoryState state, String factsJson) {
        this.summary = state.summary() == null ? "" : state.summary();
        this.factsJson = factsJson == null ? "[]" : factsJson;
        this.lastSummarizedTs = state.lastSummarizedTs();
    }
}