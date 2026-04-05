package com.discord.LocalAIDiscordAgent.llmClients.chatSummary.records;

import java.util.List;

public class SummaryRecords {

    /** Rolling summary output from the model. */
    public record SummaryUpdate(String summary) {}

    /** Facts output from the model (IDs only). */
    public record FactsUpdate(List<FactCandidate> facts) {}

    /** What the model should return. */
    public record FactCandidate(
            String key,
            String value,
            double confidence,
            List<String> evidenceTurnIds
    ) {}

    /** Evidence stored durably (survives DB trimming/deletes). */
    public record Evidence(
            String turnId,
            String ts,
            String role,
            String authorId,
            String excerpt
    ) {}

    /** What you persist in facts_json. */
    public record Fact(
            String key,
            String value,
            double confidence,
            List<Evidence> evidence
    ) {}

    public record MemoryState(
            String summary,
            List<Fact> facts,
            String lastSummarizedTs // watermark; survives trimming
    ) {}

    /**
     * A single turn in the transcript sent to the summarizer.
     * id must be STABLE (DB id, Discord snowflake, ULID...). Do NOT use array indices.
     */
    public record Turn(
            String id,
            String ts,
            String role,
            String authorId,
            String authorName,
            String content
    ) {}
}
