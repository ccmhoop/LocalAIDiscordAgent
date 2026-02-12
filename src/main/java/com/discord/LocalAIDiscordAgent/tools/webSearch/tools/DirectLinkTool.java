package com.discord.LocalAIDiscordAgent.tools.webSearch.tools;

import com.discord.LocalAIDiscordAgent.tools.webSearch.service.WebSearchMemoryService;
import com.discord.LocalAIDiscordAgent.tools.webSearch.utils.DocumentExtractionUtils;
import com.discord.LocalAIDiscordAgent.tools.webSearch.utils.NetUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * DirectLinkTool (JSON-only, direct fetch only)
 * - Only fetches the provided page.
 * - No fallback searches and no "suggested query" output.
 * - Returns JSON with status OK/ERROR.
 */
@Slf4j
@Component
public class DirectLinkTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Pattern AD_PARAMS =
            Pattern.compile("(?i)[?&](ad_domain|ad_provider|ad_type|click_metadata|ad_click|advertisement)=");

    private static final int TIMEOUT_MS = 12_000;
    private static final int MAX_BODY_BYTES = 1_000_000;
    private static final int MAX_CONTENT_LENGTH = 2_000;
    private static final int MAX_TOTAL_OUTPUT_CHARS = 200000;
    private static final int MAX_REDIRECTS = 5;

    private static final Set<String> NON_HTML_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/zip",
            "application/octet-stream",
            "image/",
            "audio/",
            "video/"
    );

    private final WebSearchMemoryService webSearchMemoryService;

    public DirectLinkTool(WebSearchMemoryService webSearchMemoryService) {
        this.webSearchMemoryService = webSearchMemoryService;
    }

    @Tool(
            name = "direct_link",
            description = """
                Fetch readable text from a single HTTP/HTTPS page (direct link).
                Only attempts to fetch the provided URL (no extra searches).
                Returns JSON only (WebSearchResponse shape).
                """)
    public String directLink(
            @ToolParam(description = "Absolute HTTP/HTTPS URL to fetch.")
            String url
    ) {
        final List<String> failedUrls = new ArrayList<>();

        String input = DocumentExtractionUtils.safeTrim(url);
        if (input.isEmpty()) {
            return clipTotal(toJson(new WebSearchResponse(
                    "SEARCH_RESULTS",
                    "",
                    "ERROR",
                    0,
                    List.of(),
                    null,
                    "Empty URL"
            )));
        }

        if (looksLikeQuery(input)) {
            return clipTotal(toJson(new WebSearchResponse(
                    "SEARCH_RESULTS",
                    input,
                    "ERROR",
                    0,
                    List.of(),
                    null,
                    "Input does not look like a URL"
            )));
        }

        String normalized = normalizeUrl(input);

        if (isAdvertisementUrl(normalized)) {
            failedUrls.add(normalized);
            return clipTotal(toJson(new WebSearchResponse(
                    "SEARCH_RESULTS",
                    input,
                    "ERROR",
                    0,
                    List.of(),
                    failedUrls,
                    "Advertisement URL blocked"
            )));
        }

        if (!isValidUrl(normalized) || !NetUtil.isSafeHttpUrl(normalized)) {
            failedUrls.add(normalized);
            return clipTotal(toJson(new WebSearchResponse(
                    "SEARCH_RESULTS",
                    input,
                    "ERROR",
                    0,
                    List.of(),
                    failedUrls,
                    "Invalid or unsafe URL"
            )));
        }

        try {
            Connection.Response resp = executeWithSafeRedirects(normalized, failedUrls);
            int httpStatus = resp.statusCode();
            String contentType = resp.contentType();
            String resolvedUrl = resp.url().toString();

            if (!NetUtil.isSafeHttpUrl(resolvedUrl)) {
                failedUrls.add(resolvedUrl);
                return clipTotal(toJson(new WebSearchResponse(
                        "SEARCH_RESULTS",
                        input,
                        "ERROR",
                        0,
                        List.of(),
                        failedUrls,
                        "Unsafe final URL after redirects"
                )));
            }

            if (isAdvertisementUrl(resolvedUrl)) {
                failedUrls.add(resolvedUrl);
                return clipTotal(toJson(new WebSearchResponse(
                        "SEARCH_RESULTS",
                        input,
                        "ERROR",
                        0,
                        List.of(),
                        failedUrls,
                        "Advertisement URL after redirects"
                )));
            }

            if (httpStatus >= 400) {
                failedUrls.add(resolvedUrl);
                return clipTotal(toJson(new WebSearchResponse(
                        "SEARCH_RESULTS",
                        input,
                        "ERROR",
                        0,
                        List.of(),
                        failedUrls,
                        "HTTP " + httpStatus
                )));
            }

            if (!isHtmlOrPlainText(contentType)) {
                failedUrls.add(resolvedUrl);
                return clipTotal(toJson(new WebSearchResponse(
                        "SEARCH_RESULTS",
                        input,
                        "ERROR",
                        0,
                        List.of(),
                        failedUrls,
                        "Non-HTML content type"
                )));
            }

            Document doc = resp.parse();
            DocumentExtractionUtils.removeBoilerplate(doc);

            String title = DocumentExtractionUtils.safeTrim(doc.title());
            String extracted = DocumentExtractionUtils.extractMainText(doc);
            String desc = DocumentExtractionUtils.extractDescription(doc);

            // lightweight "snippet"
            String snippet = DocumentExtractionUtils.safeTrim(desc);

            if (!snippet.isBlank() && extracted.length() < 400 && !extracted.contains(snippet)) {
                extracted = (snippet + "\n" + extracted).trim();
            }

            boolean truncated = extracted.length() > MAX_CONTENT_LENGTH;
            String excerpt = truncated ? DocumentExtractionUtils.clip(extracted, MAX_CONTENT_LENGTH) : extracted;

            if (DocumentExtractionUtils.safeTrim(excerpt).isEmpty()) {
                failedUrls.add(resolvedUrl);
                return clipTotal(toJson(new WebSearchResponse(
                        "SEARCH_RESULTS",
                        input,
                        "ERROR",
                        0,
                        List.of(),
                        failedUrls,
                        "No readable text found"
                )));
            }

            WebSearchResponse ok = new WebSearchResponse(
                    "SEARCH_RESULTS",
                    input, // keep original input; your memory service uses this as "query"
                    "OK",
                    1,
                    List.of(new WebSearchResponse.Result(
                            1,
                            title.isEmpty() ? resolvedUrl : title,
                            resolvedUrl,
                            snippet,  // can be ""
                            excerpt   // bounded content
                    )),
                    failedUrls.isEmpty() ? null : failedUrls,
                    null
            );

            String json = clipTotal(toJson(ok));

            try {
                // Now compatible with your JSON-only WebSearchMemoryService parser
                webSearchMemoryService.saveWebSearchResult(json);
            } catch (Exception e) {
                log.warn("Error saving direct_link JSON to memory: {}", e.getMessage());
            }

            return json;

        } catch (Exception e) {
            failedUrls.add(normalized);
            return clipTotal(toJson(new WebSearchResponse(
                    "SEARCH_RESULTS",
                    input,
                    "ERROR",
                    0,
                    List.of(),
                    failedUrls,
                    DocumentExtractionUtils.safeError(e.getMessage())
            )));
        }
    }

    // ---------------------------
    // Safe redirects
    // ---------------------------

    private Connection.Response executeWithSafeRedirects(String startUrl, List<String> failedUrls) throws IOException {
        String current = startUrl;

        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            if (!NetUtil.isSafeHttpUrl(current)) {
                DocumentExtractionUtils.safeAdd(failedUrls, current);
                throw new IOException("Unsafe URL (SSRF blocked): " + current);
            }

            Connection.Response resp = Jsoup.connect(current)
                    .timeout(TIMEOUT_MS)
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

    // ---------------------------
    // Helpers
    // ---------------------------

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

    private boolean isValidUrl(String url) {
        if (url == null) return false;

        try {
            URI uri = URI.create(url);

            String scheme = uri.getScheme();
            if (scheme == null) return false;

            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) return false;

            return uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean looksLikeQuery(String input) {
        if (input == null) return true;
        if (input.chars().anyMatch(Character::isWhitespace)) return true;
        return !(input.contains(".") || input.contains("/"));
    }

    private String normalizeUrl(String raw) {
        String v = DocumentExtractionUtils.safeTrim(raw);
        if (v.isEmpty()) return v;

        if (v.startsWith("//")) return "https:" + v;

        try {
            URI u = URI.create(v);
            if (u.getScheme() != null) return v;
        } catch (IllegalArgumentException ignored) {}

        return "https://" + v;
    }

    private boolean isHtmlOrPlainText(String contentType) {
        if (contentType == null || contentType.isBlank()) return true;
        String ct = contentType.toLowerCase(Locale.ROOT);

        if (ct.contains("text/html") || ct.contains("application/xhtml+xml")) return true;
        if (ct.contains("text/plain")) return true;

        for (String bad : NON_HTML_CONTENT_TYPES) {
            if (bad.endsWith("/") && ct.startsWith(bad)) return false;
            if (!bad.endsWith("/") && ct.startsWith(bad)) return false;
        }

        return true;
    }
    private String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private String clipTotal(String s) {
        return DocumentExtractionUtils.clipTotal(s, MAX_TOTAL_OUTPUT_CHARS);
    }

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
