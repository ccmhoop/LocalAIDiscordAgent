package com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.helpers;

import org.springframework.ai.document.Document;

import java.util.*;

public final class WebSearchChunkMerger {

    private WebSearchChunkMerger() {}

    public record MergedContent(
            String content,
            Map<String, Object> metadata
    ) {}

    public static List<MergedContent> mergeByArticle(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        Map<String, List<Document>> grouped = new LinkedHashMap<>();

        for (Document doc : docs) {
            if (doc == null) continue;

            Map<String, Object> meta = doc.getMetadata();
            Object parentId = meta.get("parent_document_id");

            String key = parentId != null
                    ? parentId.toString()
                    : doc.getId();

            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(doc);
        }

        List<MergedContent> merged = new ArrayList<>(grouped.size());

        for (List<Document> group : grouped.values()) {
            group.sort(Comparator.comparingInt(WebSearchChunkMerger::extractChunkIndex));

            StringBuilder combined = new StringBuilder();

            Map<String, Object> meta = group.getFirst().getMetadata();
            Map<String, Object> metaCopy = new HashMap<>(meta);

            for (Document d : group) {
                combined.append(d.getFormattedContent()).append("\n\n");
            }

            merged.add(new MergedContent(
                    combined.toString().trim(),
                    metaCopy
            ));
        }

        return merged;
    }

    public static List<Document> mergeToDocuments(List<Document> docs) {
        List<MergedContent> merged = mergeByArticle(docs);
        if (merged.isEmpty()) return List.of();

        List<Document> out = new ArrayList<>(merged.size());
        for (MergedContent m : merged) {
            out.add(new Document(m.content(), new HashMap<>(m.metadata())));
        }
        return out;
    }

    private static int extractChunkIndex(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        if (meta.isEmpty()) {
            return 0;
        }

        Object v = meta.getOrDefault("chunkIndex",
                meta.getOrDefault("chunk_index", 0));

        if (v instanceof Number n) return n.intValue();

        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
