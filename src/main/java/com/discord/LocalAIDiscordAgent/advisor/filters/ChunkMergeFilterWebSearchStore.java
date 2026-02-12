package com.discord.LocalAIDiscordAgent.advisor.filters;

import com.discord.LocalAIDiscordAgent.tools.webSearch.helpers.WebSearchChunkMerger;
import lombok.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;

import java.util.*;
import java.util.function.Consumer;

/**
 * VectorStore decorator:
 * - Forces WEB_SEARCH tier filtering
 * - Merges chunked documents into per-article documents
 * - Returns merged results as a SINGLE JSON document (instead of many docs)
 *
 * Why JSON doc?
 * - Easier to pass to LLM as one compact context block
 * - Avoids repeated metadata noise across many chunks
 */
public final class ChunkMergeFilterWebSearchStore implements VectorStore {

    private static final String TIER_WEB_SEARCH = "WEB_SEARCH";

    private static final int SEARCH_TOP_K = 3;
    private static final double RETRIEVAL_SIMILARITY_THRESHOLD = 0.95;

    private static final String WEB_SEARCH_FILTER_EXPRESSION = "tier == '" + TIER_WEB_SEARCH + "'";

    // JSON shaping (tune to taste)
    private static final int JSON_MAX_ARTICLES = 3;
    private static final int JSON_MAX_CHARS_PER_ARTICLE = 3500;

    private final VectorStore delegate;

    public ChunkMergeFilterWebSearchStore(@NonNull VectorStore delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate VectorStore must not be null");
    }

    @Override
    @NonNull
    public String getName() {
        return delegate.getName();
    }

    @Override
    @NonNull
    public <T> Optional<T> getNativeClient() {
        return delegate.getNativeClient();
    }

    @Override
    public void add(@NonNull List<Document> documents) {
        delegate.add(documents);
    }

    @Override
    public void write(@NonNull List<Document> documents) {
        delegate.write(documents);
    }

    @Override
    public void accept(@NonNull List<Document> documents) {
        delegate.accept(documents);
    }

    @Override
    public void delete(@NonNull List<String> idList) {
        delegate.delete(idList);
    }

    @Override
    public void delete(@NonNull Expression filterExpression) {
        delegate.delete(filterExpression);
    }

    @Override
    public void delete(@NonNull String filterExpression) {
        delegate.delete(filterExpression);
    }

    /**
     * Uses the incoming SearchRequest as-is, but merges and returns one JSON Document.
     */
    @Override
    @NonNull
    public List<Document> similaritySearch(@NonNull SearchRequest request) {
        List<Document> raw = delegate.similaritySearch(request);

        List<Document> filtered = filterToWebSearchTier(raw);
        if (filtered.isEmpty()) return List.of();

        String json = WebSearchChunkMerger.mergeByArticleToJson(
                filtered,
                JSON_MAX_ARTICLES,
                JSON_MAX_CHARS_PER_ARTICLE
        );

        return List.of(buildJsonDocument(json, filtered));
    }

    /**
     * Convenience search: forces WEB_SEARCH tier and sensible thresholds.
     * Returns one JSON Document.
     */
    @Override
    @NonNull
    public List<Document> similaritySearch(@NonNull String query) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(SEARCH_TOP_K)
                .similarityThreshold(RETRIEVAL_SIMILARITY_THRESHOLD)
                .filterExpression(WEB_SEARCH_FILTER_EXPRESSION)
                .build();

        return similaritySearch(request);
    }

    @Override
    @NonNull
    public Consumer<List<Document>> andThen(@NonNull Consumer<? super List<Document>> after) {
        return docs -> {
            accept(docs);
            after.accept(docs);
        };
    }

    private Document buildJsonDocument(String json, List<Document> sourceDocs) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("tier", TIER_WEB_SEARCH);
        meta.put("format", "json");
        meta.put("type", "MERGED_WEB_RESULTS");
        meta.put("sourceCount", sourceDocs.size());
        meta.put("timestamp", System.currentTimeMillis());

        // Keep some helpful provenance (optional)
        // If you want to avoid any extra metadata, delete this section.
        Set<String> urls = new LinkedHashSet<>();
        for (Document d : sourceDocs) {
            Object url = d.getMetadata().get("url");
            if (url != null) urls.add(url.toString());
        }
        if (!urls.isEmpty()) meta.put("urls", urls);

        return new Document(json, meta);
    }

    private List<Document> filterToWebSearchTier(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        List<Document> out = new ArrayList<>(docs.size());
        for (Document d : docs) {
            if (d == null) continue;

            Object tier = d.getMetadata().get("tier");
            if (TIER_WEB_SEARCH.equals(tier)) {
                out.add(d);
            }
        }

        return out;
    }
}
