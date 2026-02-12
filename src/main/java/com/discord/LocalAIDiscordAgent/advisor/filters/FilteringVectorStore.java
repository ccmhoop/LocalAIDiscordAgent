package com.discord.LocalAIDiscordAgent.advisor.filters;

import lombok.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FilteringVectorStore implements VectorStore {

    private static final int MIN_CONTENT_LENGTH = 20;
    private static final int MAX_CONTENT_LENGTH = 4_000;

    private static final Pattern INSTRUCTION_MARKERS = Pattern.compile(
            "(?is)\\b(ignore previous|system prompt|you are an ai|act as|follow these rules)\\b"
    );

    private static final Pattern FAILURE_MARKERS = Pattern.compile(
            "(?is)\\b(NO_CONTENT|NOT_FOUND|TOOL[_\\s-]?ERROR|FAILED|EXCEPTION|STACKTRACE)\\b"
    );

    private final VectorStore delegate;

    public FilteringVectorStore(VectorStore delegate) {
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
    public void add(List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }

        List<Document> filtered = documents.stream()
                .filter(Objects::nonNull)
                .filter(this::shouldStore)
                .collect(Collectors.toList());

        if (!filtered.isEmpty()) {
            delegate.add(filtered);
        }
    }

    @Override
    public void write(@NonNull List<Document> documents) {
        add(documents); // enforce filtering
    }

    @Override
    public void accept(@NonNull List<Document> documents) {
        add(documents); // Consumer interface → same behavior
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
        return delegate.similaritySearch(request);
    }

    @Override
    @NonNull
    public List<Document> similaritySearch(@NonNull String query) {
        return delegate.similaritySearch(query);
    }

    @Override
    @NonNull
    public Consumer<List<Document>> andThen(@NonNull Consumer<? super List<Document>> after) {
        return docs -> {
            accept(docs);
            after.accept(docs);
        };
    }


    private boolean shouldStore(Document doc) {
        String content = doc.getFormattedContent();
        if (!StringUtils.hasText(content)) {
            return false;
        }

        String text = content.trim();

        if (text.length() < MIN_CONTENT_LENGTH || text.length() > MAX_CONTENT_LENGTH) {
            return false;
        }

        if (INSTRUCTION_MARKERS.matcher(text).find()) {
            return false;
        }

        if (FAILURE_MARKERS.matcher(text).find()) {
            return false;
        }

        Map<String, Object> metadata = doc.getMetadata();
        Object role = metadata.get("role");
        if ("system".equals(role) || "tool".equals(role)) {
            return false;
        }

        Object source = metadata.get("source");
        return source == null || !source.toString().toLowerCase().contains("error");
    }
}
