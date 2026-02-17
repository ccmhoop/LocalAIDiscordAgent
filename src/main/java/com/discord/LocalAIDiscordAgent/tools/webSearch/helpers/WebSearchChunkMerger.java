package com.discord.LocalAIDiscordAgent.tools.webSearch.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;

import java.util.*;

public final class WebSearchChunkMerger {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WebSearchChunkMerger() {}

    // --- Records (typed output) ---

    public record MergedWebResults(
            String type,
            int count,
            List<MergedWebResultItem> results
    ) {
        public static MergedWebResults empty() {
            return new MergedWebResults("MERGED_WEB_RESULTS", 0, List.of());
        }
    }

    public record MergedWebResultItem(
            int rank,
            String title,
            String domain,
            String url,
            String content
    ) {}

    /**
     * Merge chunked Documents into per-article typed result.
     */
    public static MergedWebResults mergeByArticle(List<Document> docs,
                                                  int maxArticles,
                                                  int maxCharsPerArticle) {

        if (docs == null || docs.isEmpty()) {
            return MergedWebResults.empty();
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

        List<MergedWebResultItem> results = new ArrayList<>();
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

            results.add(new MergedWebResultItem(
                    rank++,
                    title,
                    domain,
                    url,
                    combined
            ));
        }

        return new MergedWebResults("MERGED_WEB_RESULTS", results.size(), List.copyOf(results));
    }

    /**
     * Optional: keep JSON boundary if some caller still expects JSON.
     */
    public static String mergeByArticleToJson(List<Document> docs,
                                              int maxArticles,
                                              int maxCharsPerArticle) {
        return toJson(mergeByArticle(docs, maxArticles, maxCharsPerArticle));
    }

    // --- Helpers ---

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

    private static String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }
}
