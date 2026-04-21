package com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.records;

import java.util.List;

public class WebSearchRecords {

    public record WebQAMemory(
            String type,
            int count,
            List<MergedWebQAItem> results
    ) {
        public static WebQAMemory empty() {
            return new WebQAMemory("MERGED_WEB_RESULTS", 0, List.of());
        }
    }

    public record MergedWebQAItem(
            int rank,
            String title,
            String domain,
            String url,
            String content
    ) {}
}
