package com.discord.LocalAIDiscordAgent.webSearch.helpers;

import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.WebQAMemory;
import org.springframework.ai.document.Document;

import java.util.*;

public final class WebSearchChunkMerger {

    private WebSearchChunkMerger() {}

    public static WebQAMemory mergeByArticle(List<Document> docs,
                                             int maxArticles,
                                             int maxCharsPerArticle) {
        if (docs == null || docs.isEmpty()) {
            return WebQAMemory.empty();
        }

        // Preserve insertion order (similarity order)
        Map<String, List<Document>> grouped = new LinkedHashMap<>();

        for (Document d : docs) {
            if (d == null) continue;

            Map<String, Object> meta = d.getMetadata();
            Object parentId = meta.get("parent_document_id");

            String key;
            if (parentId != null) {
                key = parentId.toString();
            } else {
                Object url = meta.get("url");
                key = (url != null && !url.toString().isBlank())
                        ? url.toString()
                        : d.getId();
            }

            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }

        List<MergedWebQAItem> results = new ArrayList<>();
        int rank = 1;

        for (List<Document> group : grouped.values()) {
            if (rank > maxArticles) break;

            group.sort(Comparator.comparingInt(WebSearchChunkMerger::extractChunkIndex));

            Document first = group.get(0);
            Map<String, Object> meta = first.getMetadata();

            String title = asString(meta.get("title"));
            String domain = asString(meta.get("domain"));
            String url = asString(meta.get("url"));

            String combined = combineTexts(group);
            if (combined.isBlank()) continue;

            if (maxCharsPerArticle > 0 && combined.length() > maxCharsPerArticle) {
                combined = combined.substring(0, maxCharsPerArticle) + "…";
            }

            results.add(new MergedWebQAItem(
                    rank++,
                    title,
                    domain,
                    url,
                    combined
            ));
        }

        return new WebQAMemory("MERGED_WEB_RESULTS", results.size(), List.copyOf(results));
    }


    private static String combineTexts(List<Document> group) {
        StringBuilder sb = new StringBuilder();
        for (Document d : group) {
            if (d == null) continue;
            String t = d.getText();
            if (t == null || t.isBlank()) continue;

            if (!sb.isEmpty()) sb.append("\n\n");
            sb.append(t.trim());
        }
        return sb.toString().trim();
    }

    private static int extractChunkIndex(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        if (meta == null || meta.isEmpty()) return 0;

        Object v = meta.getOrDefault("chunkIndex", meta.getOrDefault("chunk_index", 0));
        if (v instanceof Number n) return n.intValue();

        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String asString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

}
