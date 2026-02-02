package com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.service;

import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.helpers.WebSearchChunkMerger;
import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.helpers.WebSearchChunkMerger.MergedContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * WebSearchMemoryService
 * - Extract: parse tool output into WebSearchData
 * - Transform: TokenTextSplitter (token-aware chunking) + dedupe + chunk indexing
 * - Load: VectorStore.write(...)
 * - Current Implementation is LLM Generated
 */
@Slf4j
@Service
public class WebSearchMemoryService {

    private static final String TIER_WEB_SEARCH = "WEB_SEARCH";
    private static final int MAX_CLEANED_CHARS = 40_000; // hard cap safety

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+\\b");

    private static final Pattern BOILERPLATE_LINE = Pattern.compile(
                    "(?i)^\\s*(skip to|cookie|cookies|privacy|terms|consent|subscribe|sign in|sign up|login|" +
                            "newsletter|advertis|advertisement|promo|share|follow us|all rights reserved|©)\\b.*"
            );

    // Retrieval behavior
    private static final int SEARCH_TOP_K = 5;
    private static final int RESPONSE_MAX_RESULTS = 3;
    private static final double RETRIEVAL_SIMILARITY_THRESHOLD = 0.95;

    // Ingestion / dedupe behavior
    private static final int DEDUPE_TOP_K = 3;
    private static final double DEDUPE_SIMILARITY_THRESHOLD = 0.95;

    private static final int CHUNK_SIZE_TOKENS = 256;
    private static final int MIN_CHUNK_SIZE_CHARS = 100;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 100;
    private static final int MAX_NUM_CHUNKS = 10_000;
    private static final boolean KEEP_SEPARATOR = true;

    private final VectorStore vectorStore;
    private final DocumentTransformer splitter;
    private final DocumentWriter writer;

    public WebSearchMemoryService(VectorStore vectorStoreWebSearchMemory) {
        this.vectorStore = vectorStoreWebSearchMemory;
        this.writer = vectorStoreWebSearchMemory;

        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE_TOKENS)
                .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
                .withMinChunkLengthToEmbed(MIN_CHUNK_LENGTH_TO_EMBED)
                .withMaxNumChunks(MAX_NUM_CHUNKS)
                .withKeepSeparator(KEEP_SEPARATOR)
                .build();
    }

    /**
     * Searches cached web results in the vector store for the given query.
     * @return formatted response, or null if nothing relevant.
     */
    public String searchExistingContent(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        try {
            List<Document> matches = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(SEARCH_TOP_K)
                            .similarityThreshold(RETRIEVAL_SIMILARITY_THRESHOLD)
                            .filterExpression("tier == '" + TIER_WEB_SEARCH + "'")
                            .build()
            );

            if (matches.isEmpty()) {
                return null;
            }

            List<MergedContent> merged = WebSearchChunkMerger.mergeByArticle(matches);
            return formatMergedWebResponse(query, merged);

        } catch (Exception e) {
            log.error("Error searching existing content: {}", e.getMessage(), e);
            return null;
        }
    }

    private String formatMergedWebResponse(String query, List<MergedContent> articles) {
        StringBuilder out = new StringBuilder();

        int count = 0;
        for (WebSearchChunkMerger.MergedContent article : articles) {
            if (count++ >= RESPONSE_MAX_RESULTS) break;

            Map<String, Object> meta = article.metadata();

            out.append("Source: ")
                    .append(meta.getOrDefault("title", "Unknown title"))
                    .append("\n");

            out.append("Domain: ")
                    .append(meta.getOrDefault("domain", "unknown"))
                    .append("\n");

            out.append("URL: ")
                    .append(meta.getOrDefault("url", "unknown"))
                    .append("\n");

            out.append("Relevant to query: \"")
                    .append(query)
                    .append("\"\n\n");

            out.append(article.content()).append("\n\n---\n\n");
        }

        return out.toString().trim();
    }


    /**
     * Ingests tool output (WEBPAGE_FETCH or SEARCH_RESULTS) into the vector store.
     */
    public void saveWebSearchResult(String webSearchResult) {
        if (webSearchResult == null || webSearchResult.isBlank()) {
            log.debug("Empty web search result, skipping save");
            return;
        }

        List<WebSearchData> parsed = parse(webSearchResult);
        if (parsed.isEmpty()) {
            log.debug("No parsable web results found, skipping save");
            return;
        }

        int ingested = 0;
        for (WebSearchData data : parsed) {
            if (!data.isOk() || data.content().isBlank() || data.resolvedUrl().isBlank()) {
                continue;
            }
            ingest(data);
            ingested++;
        }

        log.debug("Ingested {} web search items into tier={}", ingested, TIER_WEB_SEARCH);
    }

    // -----------------------------
    // ETL pipeline (Extract/Transform/Load)
    // -----------------------------

    private void ingest(WebSearchData data) {
        DocumentReader reader = () -> List.of(buildSourceDocument(data));

        List<Document> docs = reader.read();

        List<Document> splitDocs = splitter.apply(docs);

        List<Document> ready = dedupeAndIndex(splitDocs, data.resolvedUrl());

        if (!ready.isEmpty()) {
            writer.write(ready);
        }
    }

    private Document buildSourceDocument(WebSearchData data) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tier", TIER_WEB_SEARCH);
        metadata.put("url", data.resolvedUrl());
        metadata.put("title", data.title());
        metadata.put("domain", extractDomain(data.resolvedUrl()));
        metadata.put("timestamp", System.currentTimeMillis());

        if (!data.inputUrl().isBlank() && !data.inputUrl().equals(data.resolvedUrl())) {
            metadata.put("originalUrl", data.inputUrl());
        }
        String cleaned = sanitizeWebText(data.content());

        return new Document(cleaned, metadata);
    }

    private String sanitizeWebText(String raw) {
        if (raw == null || raw.isBlank()) return "";

        // Normalize whitespace + remove NBSP
        String s = raw.replace('\u00A0', ' ');

        // Remove raw URLs anywhere
        s = URL_PATTERN.matcher(s).replaceAll("");

        // Line-based cleanup (kills banners/nav/link dumps)
        List<String> kept = new ArrayList<>();
        int emptyRun = 0;

        for (String line : s.split("\\R")) {
            String l = line.strip();

            // drop boilerplate + very-short menu-ish lines
            if (l.isEmpty()) {
                emptyRun++;
                if (emptyRun <= 1) kept.add("");
                continue;
            }
            emptyRun = 0;

            if (BOILERPLATE_LINE.matcher(l).matches()) continue;

            // Drop lines that are mostly punctuation/symbols (common in nav separators)
            int letters = 0;
            for (int i = 0; i < l.length(); i++) {
                if (Character.isLetterOrDigit(l.charAt(i))) letters++;
            }
            if ((double) letters / l.length() < 0.25 && l.length() < 120) {
                continue;
            }

            kept.add(l);
        }

        // Re-join, collapse whitespace a bit
        String cleaned = String.join("\n", kept)
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        if (cleaned.length() > MAX_CLEANED_CHARS) {
            cleaned = cleaned.substring(0, MAX_CLEANED_CHARS).trim();
        }

        return cleaned;
    }

    private List<Document> dedupeAndIndex(List<Document> docs, String sourceUrl) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        List<Document> kept = new ArrayList<>(docs.size());

        for (Document d : docs) {
            String chunk = d.getText();
            if (chunk == null || chunk.isBlank()) {
                continue;
            }

            if (!isDuplicateByVector(chunk, sourceUrl)) {
                kept.add(d);
            }
        }

        int total = kept.size();
        if (total == 0) {
            return List.of();
        }

        List<Document> indexed = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            Document original = kept.get(i);
            Map<String, Object> meta = new HashMap<>(original.getMetadata());
            meta.put("chunkIndex", i);
            meta.put("totalChunks", total);

            assert original.getText() != null;
            indexed.add(new Document(original.getText(), meta));
        }

        return indexed;
    }

    /**
     * Duplicate rule:
     * - If we find ANY chunk in WEB_SEARCH tier above a high similarity threshold, treat it as duplicate.
     * - Additionally, if the match is from the same URL, it’s definitely duplicate.
     */
    private boolean isDuplicateByVector(String chunk, String sourceUrl) {
        try {
            List<Document> matches = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(chunk)
                            .topK(DEDUPE_TOP_K)
                            .similarityThreshold(DEDUPE_SIMILARITY_THRESHOLD)
                            .filterExpression("tier == '" + TIER_WEB_SEARCH + "'")
                            .build()
            );

            if (matches.isEmpty()) {
                return false;
            }

            for (Document m : matches) {
                Object url = m.getMetadata().get("url");
                if (sourceUrl != null && sourceUrl.equals(url)) {
                    return true;
                }
            }

            return true;
        } catch (Exception e) {
            log.debug("Dedupe check failed, will save chunk. Reason: {}", e.getMessage());
            return false;
        }
    }

    private List<WebSearchData> parse(String raw) {
        if (raw.startsWith("WEBPAGE_FETCH")) {
            return parseWebPageFetch(raw).map(List::of).orElseGet(List::of);
        }
        if (raw.startsWith("SEARCH_RESULTS")) {
            return parseSearchResults(raw);
        }

        log.debug("Unknown web search result format, skipping save");
        return List.of();
    }

    private Optional<WebSearchData> parseWebPageFetch(String raw) {
        try {
            String status = "";
            String inputUrl = "";
            String resolvedUrl = "";
            String title = "";
            StringBuilder content = new StringBuilder();

            boolean inContent = false;

            for (String line : raw.split("\n")) {
                String l = line.trim();

                if (l.startsWith("Status: ")) {
                    status = l.substring("Status: ".length()).trim();
                    continue;
                }
                if (l.startsWith("Input: ")) {
                    inputUrl = l.substring("Input: ".length()).trim();
                    continue;
                }
                if (l.startsWith("ResolvedURL: ")) {
                    resolvedUrl = l.substring("ResolvedURL: ".length()).trim();
                    continue;
                }
                if (l.startsWith("Title: ")) {
                    title = l.substring("Title: ".length()).trim();
                    if ("(none)".equals(title)) title = "";
                    continue;
                }
                if (l.startsWith("Content: ")) {
                    inContent = true;
                    String start = l.substring("Content: ".length());
                    if (!"(no readable text found)".equals(start)) {
                        content.append(start);
                    }
                    continue;
                }

                if (inContent) {
                    // stop on tool footer-ish hints (keeps your old behavior but simpler)
                    if (l.startsWith("Some potentially relevant")) {
                        break;
                    }
                    if (!content.isEmpty()) content.append('\n');
                    content.append(line);
                }
            }

            WebSearchData data = new WebSearchData(
                    status,
                    inputUrl,
                    resolvedUrl,
                    title,
                    content.toString().trim()
            );

            if (data.status().isBlank() || data.content().isBlank()) {
                return Optional.empty();
            }

            return Optional.of(data);
        } catch (Exception e) {
            log.error("Error parsing WEBPAGE_FETCH: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private List<WebSearchData> parseSearchResults(String raw) {
        try {
            // Header status (skip if error)
            String status = "";
            for (String line : raw.split("\n")) {
                String l = line.trim();
                if (l.startsWith("Status: ")) {
                    status = l.substring("Status: ".length()).trim();
                }
                if (l.startsWith("---")) break;
            }
            if ("ERROR".equalsIgnoreCase(status)) {
                return List.of();
            }

            List<WebSearchData> out = new ArrayList<>();

            String title = "";
            String url = "";
            StringBuilder text = new StringBuilder();
            boolean inResult = false;

            for (String line : raw.split("\n")) {
                String l = line.trim();

                if (l.startsWith("Result ")) {
                    // flush previous
                    if (inResult && !url.isBlank() && !text.isEmpty()) {
                        out.add(new WebSearchData("OK", url, url, title, text.toString().trim()));
                    }
                    // reset
                    inResult = true;
                    title = "";
                    url = "";
                    text = new StringBuilder();
                    continue;
                }

                if (!inResult) continue;

                if (l.startsWith("Title: ")) {
                    title = l.substring("Title: ".length()).trim();
                    continue;
                }
                if (l.startsWith("URL: ")) {
                    url = l.substring("URL: ".length()).trim();
                    continue;
                }

                // Keep Snippet/Excerpt as “content” (this is what you were embedding anyway).
                if (l.startsWith("Snippet: ")) {
                    appendWithSpace(text, l.substring("Snippet: ".length()).trim());
                    continue;
                }
                if (l.startsWith("Excerpt: ")) {
                    String ex = l.substring("Excerpt: ".length()).trim();
                    // drop obvious “failed/skipped” excerpt markers
                    if (ex.contains("Failed") || ex.contains("Skipped")) continue;
                    appendWithSpace(text, ex);
                    continue;
                }

                // Continuation lines: accept non-empty lines until next delimiter-ish line
                if (!l.isEmpty() && !l.startsWith("---") && !l.startsWith("Some potentially relevant")) {
                    appendWithSpace(text, line.trim());
                }
            }

            // flush last
            if (inResult && !url.isBlank() && !text.isEmpty()) {
                out.add(new WebSearchData("OK", url, url, title, text.toString().trim()));
            }

            return out;
        } catch (Exception e) {
            log.error("Error parsing SEARCH_RESULTS: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private void appendWithSpace(StringBuilder sb, String s) {
        if (s == null || s.isBlank()) return;
        if (!sb.isEmpty()) sb.append(' ');
        sb.append(s.trim());
    }

    private String extractDomain(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return "";
        }

        String s = urlString.trim();

        // If it's missing a scheme, URI#getHost() will be null.
        // Add a default scheme so host parsing works.
        if (!s.contains("://")) {
            s = "https://" + s;
        }

        try {
            URI uri = new URI(s);

            // Normalize and extract host
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return "";
            }

            // Optional: strip leading "www."
            // host = host.startsWith("www.") ? host.substring(4) : host;

            return host;
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private record WebSearchData(
            String status,
            String inputUrl,
            String resolvedUrl,
            String title,
            String content
    ) {
        boolean isOk() {
            return "OK".equalsIgnoreCase(status);
        }

        WebSearchData {
            status = status == null ? "" : status;
            inputUrl = inputUrl == null ? "" : inputUrl;
            resolvedUrl = resolvedUrl == null ? "" : resolvedUrl;
            title = title == null ? "" : title;
            content = content == null ? "" : content;
        }
    }
}
