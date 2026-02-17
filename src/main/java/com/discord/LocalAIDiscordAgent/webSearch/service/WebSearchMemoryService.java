package com.discord.LocalAIDiscordAgent.webSearch.service;

import com.discord.LocalAIDiscordAgent.webSearch.helpers.WebSearchChunkMerger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

@Slf4j
@Service
public class WebSearchMemoryService {

    private static final String TIER_WEB_SEARCH = "WEB_SEARCH";
    private static final int MAX_CLEANED_CHARS = 800; // NOTE: truncates stored text

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+\\b");

    private static final Pattern BOILERPLATE_LINE = Pattern.compile(
            "(?i)^\\s*(skip to|cookie|cookies|privacy|terms|consent|subscribe|sign in|sign up|login|" +
                    "newsletter|advertis|advertisement|promo|share|follow us|all rights reserved|©)\\b.*"
    );

    private static final Pattern ISO_DATE_TIME = Pattern.compile(
            "\\b(\\d{4}-\\d{2}-\\d{2})T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?\\b"
    );

    private static final Pattern SLASH_DATE = Pattern.compile(
            "\\s*/\\s*(\\d{4}-\\d{2}-\\d{2})\\b"
    );

    private static final Pattern AD_PARAMS = Pattern.compile("(?i)[?&](ad_domain|ad_provider|ad_type|click_metadata|ad_click|advertisement)=");

    private static final int SEARCH_TOP_K = 3;
    private static final double RETRIEVAL_SIMILARITY_THRESHOLD = .50;

    private static final int DEDUPE_TOP_K = 3;
    private static final double DEDUPE_SIMILARITY_THRESHOLD = 0.95;

    private static final int CHUNK_SIZE_TOKENS = 200;
    private static final int MIN_CHUNK_SIZE_CHARS = 100;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 100;
    private static final int MAX_NUM_CHUNKS = 300;
    private static final boolean KEEP_SEPARATOR = true;

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
     * Record-based retrieval: returns a typed object (not JSON).
     * @return Merged results or null if nothing relevant.
     */
    public WebSearchChunkMerger.MergedWebResults searchExistingContent(String query) {
        if (query == null || query.isBlank()) return null;

        try {
            List<Document> matches = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(SEARCH_TOP_K)
                            .similarityThreshold(RETRIEVAL_SIMILARITY_THRESHOLD)
                            .filterExpression("tier == '" + TIER_WEB_SEARCH + "'")
                            .build()
            );

            if (matches.isEmpty()) return null;

            return WebSearchChunkMerger.mergeByArticle(
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
     * Backward-compatible wrapper if some tool boundary still expects a string.
     */
    public String searchExistingContentJson(String query) {
        WebSearchChunkMerger.MergedWebResults merged = searchExistingContent(query);
        if (merged == null) return null;
        return toJson(merged);
    }

    /**
     * Ingests tool output into the vector store.
     * Supported:
     * - JSON SEARCH_RESULTS (preferred)
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
            if (!data.isOk() || data.content().isBlank() || data.resolvedUrl().isBlank()) continue;
            ingest(data);
            ingested++;
        }

        log.debug("Ingested {} web search items into tier={}", ingested, TIER_WEB_SEARCH);
    }

    // -----------------------------
    // ETL pipeline
    // -----------------------------

    private void ingest(WebSearchData data) {
        if (isAdvertisementUrl(data.resolvedUrl()) || isAdvertisementUrl(data.inputUrl())) {
            log.debug("Skipping advertisement URL: {}", data.resolvedUrl());
            return;
        }

        DocumentReader reader = () -> List.of(buildSourceDocument(data));
        List<Document> docs = reader.read();
        List<Document> splitDocs = splitter.apply(docs);
        List<Document> ready = dedupeAndIndex(splitDocs, data.resolvedUrl());

        if (!ready.isEmpty()) writer.write(ready);
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

    private static String normalizeTimestamps(String s) {
        if (s == null || s.isBlank()) return "";
        String out = ISO_DATE_TIME.matcher(s).replaceAll("$1");   // keep only the date
        out = SLASH_DATE.matcher(out).replaceAll(" ($1)");        // " / 2024-05-16" -> " (2024-05-16)"
        return out;
    }

    private String sanitizeWebText(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String s = raw.replace('\u00A0', ' ');

        // NEW: strip milliseconds / time part
        s = normalizeTimestamps(s);

        // Remove raw URLs anywhere
        s = URL_PATTERN.matcher(s).replaceAll("");


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
            if ((double) letters / l.length() < 0.25 && l.length() < 120) continue;

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
        if (docs == null || docs.isEmpty()) return List.of();

        List<Document> kept = new ArrayList<>(docs.size());
        for (Document d : docs) {
            String chunk = d.getText();
            if (chunk == null || chunk.isBlank()) continue;

            if (!isDuplicateByVector(chunk, sourceUrl)) kept.add(d);
        }

        int total = kept.size();
        if (total == 0) return List.of();

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
                if (sourceUrl != null && sourceUrl.equals(url)) return true;
            }

            return true;
        } catch (Exception e) {
            log.debug("Dedupe check failed, will save chunk. Reason: {}", e.getMessage());
            return false;
        }
    }

    // -----------------------------
    // Parsing using records (JSON preferred)
    // -----------------------------

    private List<WebSearchData> parse(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) return List.of();

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
            SearchResultsPayload payload = objectMapper.readValue(json, SearchResultsPayload.class);

            if (!payload.isSearchResultsLike()) return List.of();
            if (!"OK".equalsIgnoreCase(payload.status())) return List.of();

            String query = safe(payload.query());
            List<SearchResultItem> results = payload.results() == null ? List.of() : payload.results();
            if (results.isEmpty()) return List.of();

            List<WebSearchData> out = new ArrayList<>();

            for (SearchResultItem r : results) {
                String title = safe(r.title());
                String url = safe(r.url());
                String snippet = safe(r.snippet());
                String excerpt = safe(r.excerpt());

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
        String q = normalizeTimestamps(safe(query));
        String t = normalizeTimestamps(safe(title));
        String sn = normalizeTimestamps(safe(snippet));
        String ex = normalizeTimestamps(safe(excerpt));

        if (ex.length() > 2000) ex = ex.substring(0, 2000) + "…";
        if (looksLikeFetchFailure(ex)) ex = "";

        StringBuilder sb = new StringBuilder(512);

        // If you want title included in the stored text (helps retrieval), keep these:
        if (!t.isBlank()) sb.append(t).append('\n');
        if (!q.isBlank()) sb.append(q).append('\n');

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
        if (urlString == null || urlString.isBlank()) return "";

        String s = urlString.trim();
        if (!s.contains("://")) s = "https://" + s;

        try {
            URI uri = new URI(s);
            String host = uri.getHost();
            return (host == null || host.isBlank()) ? "" : host;
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private boolean isAdvertisementUrl(String url) {
        if (url == null || url.isBlank()) return false;

        String urlLower = url.toLowerCase(Locale.ROOT);

        if (url.startsWith("/y.js") || urlLower.contains("duckduckgo.com/y.js")) return true;
        if (AD_PARAMS.matcher(url).find()) return true;

        return urlLower.contains("ad_click")
                || urlLower.contains("advertisement")
                || urlLower.contains("sponsored")
                || urlLower.contains("promo");
    }

    private static String toJson(Object o) {
        try {
            return new ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    // -----------------------------
    // Records used for parsing tool output
    // -----------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResultsPayload(
            String type,
            String query,
            String status,
            Integer count,
            List<SearchResultItem> results,
            List<String> failedUrls
    ) {
        boolean isSearchResultsLike() {
            // allow missing/odd "type" as long as it smells like the payload
            if (type == null || type.isBlank()) {
                return results != null; // minimal
            }
            return "SEARCH_RESULTS".equalsIgnoreCase(type);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResultItem(
            Integer rank,
            String title,
            String url,
            String snippet,
            String excerpt
    ) {}

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
