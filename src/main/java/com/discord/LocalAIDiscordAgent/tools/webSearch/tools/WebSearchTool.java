package com.discord.LocalAIDiscordAgent.tools.webSearch.tools;

import com.discord.LocalAIDiscordAgent.tools.webSearch.service.WebSearchMemoryService;

import com.discord.LocalAIDiscordAgent.tools.webSearch.utils.DocumentExtractionUtils;
import com.discord.LocalAIDiscordAgent.tools.webSearch.utils.NetUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSearchTool
 * - Pure JSON output only (no legacy SEARCH_RESULTS / WEBPAGE_FETCH strings)
 * - Keeps output bounded (MAX_TOTAL_OUTPUT_CHARS) and excerpt bounded (MAX_EXCERPT_CHARS)
 * - Skips advertisement/tracking URLs
 * - Saves JSON output directly into WebSearchMemoryService (which now parses JSON only)
 */
@Slf4j
@Component
public class WebSearchTool {

    private static final Pattern UDDG_PARAM = Pattern.compile("[?&]uddg=([^&]+)");
    private static final Pattern U3_PARAM = Pattern.compile("[?&]u3=([^&]+)");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Pattern to detect advertisement URLs
    private static final Pattern AD_PARAMS = Pattern.compile("(?i)[?&](ad_domain|ad_provider|ad_type|click_metadata|ad_click|advertisement)=");

    private static final int SEARCH_TIMEOUT_MS = 15_000;
    private static final int MAX_BODY_BYTES = 1_000_000;
    private static final int PAGE_TIMEOUT_MS = 15_000;
    private static final int MAX_RESULTS = 3;
    private static final int MAX_EXCERPT_CHARS = 1_200;
    private static final int MAX_TOTAL_OUTPUT_CHARS = 6000;
    private static final int MAX_REDIRECTS = 5;

    private final WebSearchMemoryService webSearchMemoryService;

    public WebSearchTool(WebSearchMemoryService webSearchMemoryService) {
        this.webSearchMemoryService = webSearchMemoryService;
    }

    @Tool(
            name = "web_search",
            description = """
                    Always use this tool when available.
                    Search the web when requested.
                    """
    )
    public String webSearch(
            @ToolParam(description = """
                    The search engine query.
                    Write it as a neutral, information-seeking query.
                    """)
            String query
    ) {
        String safeQuery = DocumentExtractionUtils.safeTrim(query);

        if (safeQuery.isEmpty()) {
            return buildAndMaybePersist(new WebSearchResponse(
                    "SEARCH_RESULTS",
                    safeQuery,
                    "ERROR",
                    0,
                    List.of(),
                    List.of(),
                    "Empty query"
            ));
        }

        final List<String> failedUrls = new ArrayList<>();

        // Live search
        String encoded = URLEncoder.encode(safeQuery, StandardCharsets.UTF_8);
        String searchUrl = "https://html.duckduckgo.com/html/?q=" + encoded;

        try {
            Document doc = Jsoup.connect(searchUrl)
                    .timeout(SEARCH_TIMEOUT_MS)
                    .followRedirects(true)
                    .userAgent(NetUtil.commonUserAgent())
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .referrer("https://duckduckgo.com/")
                    .ignoreContentType(true)
                    .get();

            Elements resultBlocks = doc.select("div.results > div.result, div.result");
            List<SearchResult> top = new ArrayList<>();

            for (Element r : resultBlocks) {
                if (top.size() >= MAX_RESULTS) break;

                Element a = r.selectFirst("a.result__a");
                if (a == null) a = r.selectFirst("h2 a, a[href]");
                if (a == null) continue;

                String title = DocumentExtractionUtils.safeTrim(a.text());
                if (title.isEmpty()) continue;

                String rawHref = a.attr("href");
                if (rawHref.isBlank()) continue;

                if (isAdvertisementUrl(rawHref)) {
                    log.debug("Skipping advertisement URL in search results: {}", rawHref);
                    continue;
                }

                String url = normalizeDuckDuckGoUrl(rawHref);
                if (DocumentExtractionUtils.safeTrim(url).isEmpty()) continue;

                if (isAdvertisementUrl(url)) {
                    log.debug("Skipping advertisement URL after normalization: {}", url);
                    continue;
                }

                Element snip = r.selectFirst(".result__snippet, .result__snippet--expanded, .result__body");
                String snippet = snip != null
                        ? DocumentExtractionUtils.safeTrim(snip.text())
                        : "No description available";

                top.add(new SearchResult(title, url, snippet));
                log.debug("Added good search result {}/{}: {}", top.size(), MAX_RESULTS, title);
            }

            if (top.isEmpty()) {
                return buildAndMaybePersist(new WebSearchResponse(
                        "SEARCH_RESULTS",
                        safeQuery,
                        "OK",
                        0,
                        List.of(),
                        failedUrls,
                        null
                ));
            }

            List<WebSearchResponse.Result> results = new ArrayList<>(top.size());
            for (int i = 0; i < top.size(); i++) {
                SearchResult sr = top.get(i);

                String excerpt = fetchAndExtractPage(sr.url, failedUrls);
                excerpt = clip(excerpt, 2000);

                results.add(new WebSearchResponse.Result(
                        i + 1,
                        sr.title,
                        sr.url,
                        sr.snippet,
                        excerpt
                ));
            }

            return buildAndMaybePersist(new WebSearchResponse(
                    "SEARCH_RESULTS",
                    safeQuery,
                    "OK",
                    results.size(),
                    results,
                    failedUrls.isEmpty() ? null : failedUrls,
                    null
            ));

        } catch (IOException e) {
            return buildAndMaybePersist(new WebSearchResponse(
                    "SEARCH_RESULTS",
                    safeQuery,
                    "ERROR",
                    0,
                    List.of(),
                    failedUrls.isEmpty() ? null : failedUrls,
                    "Unable to perform search: " + DocumentExtractionUtils.safeError(e.getMessage())
            ));
        }
    }

    /**
     * Builds clipped JSON, persists into WebSearchMemoryService, and returns clipped JSON.
     */
    private String buildAndMaybePersist(WebSearchResponse response) {
        String json = DocumentExtractionUtils.clipTotal(toJson(response), MAX_TOTAL_OUTPUT_CHARS);

        try {
            // Let WebSearchMemoryService handle the filtering internally, just like DirectLinkTool does
            webSearchMemoryService.saveWebSearchResult(json);
        } catch (Exception e) {
            log.warn("Failed to save SEARCH_RESULTS to memory: {}", e.getMessage());
        }

        return json;
    }

    private String fetchAndExtractPage(String url, List<String> failedUrls) {
        if (!NetUtil.isSafeHttpUrl(url)) {
            DocumentExtractionUtils.safeAdd(failedUrls, url);
            return "[Skipped: unsafe or non-http(s) URL]";
        }

        try {
            Connection.Response resp = executeWithSafeRedirects(url, failedUrls);
            int status = resp.statusCode();
            String contentType = resp.contentType();
            String resolvedUrl = resp.url().toString();

            if (!NetUtil.isSafeHttpUrl(resolvedUrl)) {
                DocumentExtractionUtils.safeAdd(failedUrls, resolvedUrl);
                return "[Skipped: unsafe final URL after redirects]";
            }

            if (isAdvertisementUrl(resolvedUrl)) {
                DocumentExtractionUtils.safeAdd(failedUrls, resolvedUrl);
                return "[Skipped: advertisement URL after redirects]";
            }

            if (status >= 400) {
                DocumentExtractionUtils.safeAdd(failedUrls, resolvedUrl);
                return "[Failed to fetch page: HTTP " + status + "]";
            }

            if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("text/plain")) {
                String body = DocumentExtractionUtils.safeTrim(resp.body());
                if (body.isEmpty()) {
                    DocumentExtractionUtils.safeAdd(failedUrls, resolvedUrl);
                    return "[Skipped: no readable text found]";
                }
                return DocumentExtractionUtils.clip(
                        DocumentExtractionUtils.normalizeWhitespace(body),
                        MAX_EXCERPT_CHARS
                );
            }

            Document doc = resp.parse();
            DocumentExtractionUtils.removeBoilerplate(doc);

            String text = DocumentExtractionUtils.extractMainText(doc);
            String desc = DocumentExtractionUtils.extractDescription(doc);
            if (!desc.isBlank() && text.length() < 400 && !text.contains(desc)) {
                text = (desc + " " + text).trim();
            }

            if (DocumentExtractionUtils.safeTrim(text).isEmpty()) {
                DocumentExtractionUtils.safeAdd(failedUrls, resolvedUrl);
                return "[Skipped: no readable text found]";
            }

            return DocumentExtractionUtils.clip(text, MAX_EXCERPT_CHARS);

        } catch (IOException e) {
            DocumentExtractionUtils.safeAdd(failedUrls, url);
            return "[Failed to fetch page: " + DocumentExtractionUtils.safeError(e.getMessage()) + "]";
        }
    }

    private Connection.Response executeWithSafeRedirects(String startUrl, List<String> failedUrls) throws IOException {
        String current = startUrl;

        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            if (!NetUtil.isSafeHttpUrl(current)) {
                DocumentExtractionUtils.safeAdd(failedUrls, current);
                throw new IOException("Unsafe URL (SSRF blocked): " + current);
            }

            Connection.Response resp = Jsoup.connect(current)
                    .timeout(PAGE_TIMEOUT_MS)
                    .followRedirects(false)
                    .maxBodySize(MAX_BODY_BYTES)
                    .userAgent(NetUtil.commonUserAgent())
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,text/plain;q=0.8,*/*;q=0.7")
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .execute();

            String next = NetUtil.getRedirectTargetOrNull(resp);
            if (next != null) {
                current = next;
                continue;
            }

            return resp;
        }

        DocumentExtractionUtils.safeAdd(failedUrls, startUrl);
        throw new IOException("Too many redirects (limit=" + MAX_REDIRECTS + ")");
    }

    // ---------- Advertisement URL Detection ----------
    private boolean isAdvertisementUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

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

    // ---------- DDG URL normalization ----------
    private String normalizeDuckDuckGoUrl(String href) {
        if (href == null) return "";
        String h = href.trim();
        if (h.isEmpty()) return "";

        if (isAdvertisementUrl(h)) return "";

        if (h.startsWith("/duckduckgo.com/")) {
            h = h.substring("/duckduckgo.com".length());
        }

        String absolute;
        if (h.startsWith("//")) {
            absolute = "https:" + h;
        } else if (h.startsWith("/")) {
            absolute = "https://duckduckgo.com" + h;
        } else {
            absolute = h;
        }

        Matcher uddg = UDDG_PARAM.matcher(absolute);
        if (uddg.find()) {
            try {
                return URLDecoder.decode(uddg.group(1), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }

        Matcher u3 = U3_PARAM.matcher(absolute);
        if (u3.find()) {
            try {
                return URLDecoder.decode(u3.group(1), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }

        return absolute;
    }

    private String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private static String clip(String s, int maxChars) {
        if (s == null) return "";
        if (maxChars <= 0 || s.length() <= maxChars) return s;
        return s.substring(0, maxChars) + "…";
    }

    private record SearchResult(String title, String url, String snippet) {
    }

    /**
     * Pure JSON response object returned by the tool.
     * Note: includes an optional "error" field for ERROR status.
     */
    private record WebSearchResponse(
            String type,                 // "SEARCH_RESULTS"
            String query,                // original URL (or normalized/resolved URL if you prefer)
            String status,               // "OK" | "ERROR"
            int count,                   // 0 or 1 for direct_link
            List<Result> results,        // single result for the page
            List<String> failedUrls,     // optional
            String error                 // error message if status == ERROR
    ) {
        private record Result(
                int rank,                // always 1
                String title,
                String url,              // resolvedUrl
                String snippet,           // short description (or "")
                String excerpt            // extracted readable text (bounded)
        ) {}
    }

}
