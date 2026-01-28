package com.discord.LocalAIDiscordAgent.aiAdvisor.filters;

import lombok.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;

import java.util.*;
import java.util.function.Consumer;

public final class MergingWebVectorStore implements VectorStore {

    private final VectorStore delegate;

    public MergingWebVectorStore(@NonNull VectorStore delegate) {
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
        List<Document> chunks = delegate.similaritySearch(request);
        return mergeChunks(chunks);
    }

    @Override
    @NonNull
    public List<Document> similaritySearch(@NonNull String query) {
        return mergeChunks(delegate.similaritySearch(query));
    }

    @Override
    @NonNull
    public Consumer<List<Document>> andThen(@NonNull Consumer<? super List<Document>> after) {
        return docs -> {
            accept(docs);
            after.accept(docs);
        };
    }

    /* =========================
       Merge logic
       ========================= */

    private List<Document> mergeChunks(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        Map<String, List<Document>> grouped = new LinkedHashMap<>();

        for (Document doc : docs) {
            Map<String, Object> meta = doc.getMetadata();

            String key =
                    meta.get("parent_document_id") != null
                            ? meta.get("parent_document_id").toString()
                            : doc.getId();

            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(doc);
        }

        List<Document> merged = new ArrayList<>();

        for (List<Document> group : grouped.values()) {
            group.sort(Comparator.comparingInt(this::extractChunkIndex));

            StringBuilder combined = new StringBuilder();
            Map<String, Object> mergedMeta =
                    new HashMap<>(group.get(0).getMetadata());

            for (Document d : group) {
                combined.append(d.getFormattedContent()).append("\n\n");
            }

            merged.add(new Document(
                    combined.toString().trim(),
                    mergedMeta
            ));
        }

        return merged;
    }

    private int extractChunkIndex(Document doc) {
        Map<String, Object> meta = doc.getMetadata();

        Object v = meta.getOrDefault(
                "chunkIndex",
                meta.getOrDefault("chunk_index", 0)
        );

        if (v instanceof Number n) {
            return n.intValue();
        }

        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
