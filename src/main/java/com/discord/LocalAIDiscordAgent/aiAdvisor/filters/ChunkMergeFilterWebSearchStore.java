package com.discord.LocalAIDiscordAgent.aiAdvisor.filters;

import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.helpers.WebSearchChunkMerger;
import lombok.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;

import java.util.*;
import java.util.function.Consumer;

public final class ChunkMergeFilterWebSearchStore implements VectorStore {

    private static final String TIER_WEB_SEARCH = "WEB_SEARCH";

    private static final int SEARCH_TOP_K = 3;
    private static final double RETRIEVAL_SIMILARITY_THRESHOLD = 0.95;

    private static final String WEB_SEARCH_FILTER_EXPRESSION = "tier == '" + TIER_WEB_SEARCH + "'";

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

    @Override
    @NonNull
    public List<Document> similaritySearch(@NonNull SearchRequest request) {
        List<Document> raw = delegate.similaritySearch(request);
        return WebSearchChunkMerger.mergeToDocuments(filterToWebSearchTier(raw));
    }

    @Override
    @NonNull
    public List<Document> similaritySearch(@NonNull String query) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(SEARCH_TOP_K)
                .similarityThreshold(RETRIEVAL_SIMILARITY_THRESHOLD)
                .filterExpression(WEB_SEARCH_FILTER_EXPRESSION)
                .build();

        List<Document> raw = delegate.similaritySearch(request);
        return WebSearchChunkMerger.mergeToDocuments(filterToWebSearchTier(raw));
    }

    @Override
    @NonNull
    public Consumer<List<Document>> andThen(@NonNull Consumer<? super List<Document>> after) {
        return docs -> {
            accept(docs);
            after.accept(docs);
        };
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
