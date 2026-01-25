package com.discord.LocalAIDiscordAgent.aiAdvisor.filters;

import lombok.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FilteringVectorStore implements VectorStore {

    private static final Pattern FAILURE_MARKERS = Pattern.compile(
            "(?is)\\b(NO_CONTENT|NOT_FOUND|TOOL[_\\s-]?ERROR|TOOL[_\\s-]?FAILED|FUNCTION[_\\s-]?CALL[_\\s-]?FAILED)\\b"
    );

    private final VectorStore delegate;

    public FilteringVectorStore(VectorStore delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate VectorStore must not be null");
    }

    @Override
    public void add(List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }

        var filtered = documents.stream()
                .filter(Objects::nonNull)
                .filter(this::shouldStore)
                .collect(Collectors.toList());

        if (!filtered.isEmpty()) {
            delegate.add(filtered);
        }
    }

    @Override
    public void delete(@NonNull List<String> idList) {
        delegate.delete(idList);
    }

    @Override
    public void delete( @NonNull Filter.Expression filterExpression) {
        delegate.delete(filterExpression);
    }

    @Override
    @NonNull
    public List<Document> similaritySearch(@NonNull String query) {
        return delegate.similaritySearch(query);
    }

    @Override
    @NonNull
    public List<Document> similaritySearch(@NonNull SearchRequest request) {
        return delegate.similaritySearch(request);
    }

    @Override
    @NonNull
    public <T> Optional<T> getNativeClient() {
        return delegate.getNativeClient();
    }

    private boolean shouldStore(Document d) {
        String text = d.getText();

        if (!StringUtils.hasText(text)) {
            return false;
        }

        String t = text.trim();
        if ("null".equalsIgnoreCase(t)) {
            return false;
        }
        if (t.equalsIgnoreCase("NO_CONTENT") || t.equalsIgnoreCase("NOT_FOUND")) {
            return false;
        }
        if (FAILURE_MARKERS.matcher(t).find()) {
            return false;
        }
        return !t.contains("WEBPAGE_FETCH") || !t.matches("(?is).*\\bStatus:\\s*ERROR\\b.*");
    }
}
