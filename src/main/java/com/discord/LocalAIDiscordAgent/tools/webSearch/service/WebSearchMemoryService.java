package com.discord.LocalAIDiscordAgent.tools.webSearch.service;

import com.discord.LocalAIDiscordAgent.tools.webSearch.helpers.WebSearchChunkMerger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 *
 * Updated to support JSON:
 * - searchExistingContent(...) returns MERGED_WEB_RESULTS JSON (via WebSearchChunkMerger.mergeByArticleToJson)
 * - saveWebSearchResult(...) ingests JSON SEARCH_RESULTS produced by your web_search tool
 * - Legacy "WEBPAGE_FETCH" and "SEARCH_RESULTS" (string) parsing kept for backward compatibility
 */
@Slf4j
@Service
public class WebSearchMemoryService {

    private static final String TIER_WEB_SEARCH = "WEB_SEARCH";
    private static final int MAX_CLEANED_CHARS = 800; // reduced to prevent context overload

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+\\b");

    private static final Pattern BOILERPLATE_LINE = Pattern.compile(
            "(?i)^\\s*(skip to|cookie|cookies|privacy|terms|consent|subscribe|sign in|sign up|login|" +
                    "newsletter|advertis|advertisement|promo|share|follow us|all rights reserved|©)\\b.*"
    );

    // Pattern to detect advertisement URLs
    private static final Pattern AD_PARAMS = Pattern.compile("(?i)[?&](ad_domain|ad_provider|ad_type|click_metadata|ad_click|advertisement)=");

    // Retrieval behavior - reduced to prevent context window overload
    private static final int SEARCH_TOP_K = 2;
    private static final double RETRIEVAL_SIMILARITY_THRESHOLD = 0.95;

    // Ingestion / dedupe behavior
    private static final int DEDUPE_TOP_K = 3;
    private static final double DEDUPE_SIMILARITY_THRESHOLD = 0.95;

    // Chunking behavior - significantly reduced to prevent context overload (256k limit)
    private static final int CHUNK_SIZE_TOKENS = 200;
    private static final int MIN_CHUNK_SIZE_CHARS = 100;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 100;
    private static final int MAX_NUM_CHUNKS = 300;
    private static final boolean KEEP_SEPARATOR = true;

    // JSON shaping for searchExistingContent
    private static final int JSON_MAX_ARTICLES = 2;
    private static final int JSON_MAX_CHARS_PER_ARTICLE = 2000;

    private final VectorStore vectorStore;
    private final DocumentTransformer splitter;
    private final DocumentWriter writer;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * @return JSON string (MERGED_WEB_RESULTS) or null if nothing relevant.
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

            // Return a compact, structured JSON blob (single string)
            return WebSearchChunkMerger.mergeByArticleToJson(
                    matches,
                    JSON_MAX_ARTICLES,
                    JSON_MAX_CHARS_PER_ARTICLE
            );

        } catch (Exception e) {
            log.error("Error searching existing content: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Ingests tool output into the vector store.
     * Supported formats:
     * - JSON SEARCH_RESULTS (preferred)
     * - Legacy string SEARCH_RESULTS
     * - Legacy string WEBPAGE_FETCH
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
        // Filter out advertisement URLs as a safety net
        if (isAdvertisementUrl(data.resolvedUrl()) || isAdvertisementUrl(data.inputUrl())) {
            log.debug("Skipping advertisement URL: {}", data.resolvedUrl());
            return;
        }

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

            if (l.isEmpty()) {
                emptyRun++;
                if (emptyRun <= 1) kept.add("");
                continue;
            }
            emptyRun = 0;

            if (BOILERPLATE_LINE.matcher(l).matches()) continue;

            int letters = 0;
            for (int i = 0; i < l.length(); i++) {
                if (Character.isLetterOrDigit(l.charAt(i))) letters++;
            }
            if ((double) letters / l.length() < 0.25 && l.length() < 120) {
                continue;
            }

            kept.add(l);
        }

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
            if (chunk == null || chunk.isBlank()) continue;

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
            indexed.add(new Document(original.getText(), meta));
        }

        return indexed;
    }

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

            if (matches.isEmpty()) return false;

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

    // -----------------------------
    // Parsing (JSON preferred)
    // -----------------------------

    private List<WebSearchData> parse(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) return List.of();

        // Preferred: JSON SEARCH_RESULTS from web_search tool
        if (looksLikeJsonSearchResults(trimmed)) {
            return parseJsonSearchResults(trimmed);
        }

        log.debug("Unknown web search result format, skipping save");
        return List.of();
    }

    private boolean looksLikeJsonSearchResults(String s) {
        if (!s.startsWith("{")) return false;
        return s.contains("\"type\"") && s.contains("SEARCH_RESULTS") && s.contains("\"results\"");
    }

    /**
     * Parses JSON output produced by your updated web_search tool.
     *
     * Expected:
     * {
     *   "type":"SEARCH_RESULTS",
     *   "query":"...",
     *   "status":"OK",
     *   "count":3,
     *   "results":[{"rank":1,"title":"...","url":"...","snippet":"...","excerpt":"..."}],
     *   "failedUrls":[...]
     * }
     */
    private List<WebSearchData> parseJsonSearchResults(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String type = root.path("type").asText("");
            if (!"SEARCH_RESULTS".equalsIgnoreCase(type)) {
                // allow missing type if it clearly has results/status
                if (!root.has("results") || !root.has("status")) return List.of();
            }

            String status = root.path("status").asText("");
            if (!"OK".equalsIgnoreCase(status)) return List.of();

            String query = root.path("query").asText("");
            JsonNode results = root.path("results");
            if (!results.isArray() || results.isEmpty()) return List.of();

            List<WebSearchData> out = new ArrayList<>();

            for (JsonNode r : results) {
                String title = r.path("title").asText("");
                String url = r.path("url").asText("");
                String snippet = r.path("snippet").asText("");
                String excerpt = r.path("excerpt").asText("");

                if (url.isBlank()) continue;
                if (isAdvertisementUrl(url)) continue;

                String content = buildEmbedContent(query, title, snippet, excerpt);
                if (content.isBlank()) continue;

                out.add(new WebSearchData("OK", url, url, title, content));
            }

            return out;

        } catch (Exception e) {
            log.debug("Error parsing JSON SEARCH_RESULTS: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildEmbedContent(String query, String title, String snippet, String excerpt) {
        String q = safe(query);
        String t = safe(title);
        String sn = safe(snippet);
        String ex = safe(excerpt);

        // truncate excerpt to avoid embedding bloat
        if (ex.length() > 2000) ex = ex.substring(0, 2000) + "…";
        if (looksLikeFetchFailure(ex)) ex = "";

        StringBuilder sb = new StringBuilder(512);
//        sb.append("WEB_SEARCH_RESULT\n");
//        if (!q.isBlank()) sb.append("Query: ").append(q).append('\n');
//        if (!t.isBlank()) sb.append("Title: ").append(t).append('\n');
//        if (!sn.isBlank()) sb.append("Snippet: ").append(sn).append('\n');
//        if (!ex.isBlank()) sb.append("Excerpt: ").append(ex).append('\n');
        if (!sn.isBlank()) sb.append(sn).append('\n');
        if (!ex.isBlank()) sb.append(ex).append('\n');

        String content = sb.toString().trim();
        if (content.length() > 6000) content = content.substring(0, 6000).trim();
        return content;
    }

    private boolean looksLikeFetchFailure(String excerpt) {
        String e = excerpt == null ? "" : excerpt.toLowerCase(Locale.ROOT);
        return e.contains("failed to fetch")
                || e.contains("http 403")
                || e.contains("http 401")
                || e.contains("skipped");
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String extractDomain(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return "";
        }

        String s = urlString.trim();
        if (!s.contains("://")) {
            s = "https://" + s;
        }

        try {
            URI uri = new URI(s);
            String host = uri.getHost();
            if (host == null || host.isBlank()) return "";
            return host;
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private boolean isAdvertisementUrl(String url) {
        if (url == null || url.isBlank()) return false;

        String urlLower = url.toLowerCase(Locale.ROOT);

        if (url.startsWith("/y.js") || urlLower.contains("duckduckgo.com/y.js")) {
            return true;
        }

        if (AD_PARAMS.matcher(url).find()) {
            return true;
        }

        return urlLower.contains("ad_click")
                || urlLower.contains("advertisement")
                || urlLower.contains("sponsored")
                || urlLower.contains("promo");
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
