package com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.tools;

import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.service.WebSearchMemoryService;
import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.utils.DocumentExtractionUtils;
import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.utils.NetUtil;
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

@Slf4j
@Component
public class DirectLinkTool {

    private static final int TIMEOUT_MS = 12_000;
    private static final int MAX_BODY_BYTES = 1_000_000;
    private static final int MAX_CONTENT_LENGTH = 2_000;
    private static final int MAX_TOTAL_OUTPUT_CHARS = 6_000;
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

    public DirectLinkTool(
            WebSearchMemoryService webSearchMemoryService
    ) {
        this.webSearchMemoryService = webSearchMemoryService;
    }

    @Tool(
            name = "Direct_Link",
            description = """
                    Fetch readable text from a single authoritative HTTP/HTTPS page (direct link).
                    Use this when the user provides a direct URL, or when you can confidently identify a specific page to read.
                    If the user provides a URL, you should use this tool instead of searching.
                   
                    - Returns title + a bounded text excerpt.
                    - Checks existing web search memory first.
                    - If the input is not a valid URL or the page cannot be fetched/read, it returns a suggested SearchQuery
                      (fallback) that you can run with web_search.
                    """)
    public String directLink(
            @ToolParam(description = "Absolute HTTP/HTTPS URL to fetch. If the value is not a valid URL, the tool will treat it as a query and return a suggested SearchQuery for use with web_search.")
            String url
    ) {
        final List<String> failedUrls = new ArrayList<>();

        String input = DocumentExtractionUtils.safeTrim(url);
        if (input.isEmpty()) {
            return "WEBPAGE_FETCH\nStatus: ERROR\nError: Empty URL/query.";
        }

        if (looksLikeQuery(input)) {
            return DocumentExtractionUtils.clipTotal(fallbackSearch(input, "Input does not look like a URL", failedUrls), MAX_TOTAL_OUTPUT_CHARS);
        }

        String normalized = normalizeUrl(input);

        if (!isValidUrl(normalized) || !NetUtil.isSafeHttpUrl(normalized)) {
            log.warn("Invalid or unsafe URL: {} (normalized: {})", input, normalized);
            failedUrls.add(normalized);
            return DocumentExtractionUtils.clipTotal(fallbackSearch(input, "Invalid or unsafe URL", failedUrls), MAX_TOTAL_OUTPUT_CHARS);
        }

        try {
            Connection.Response resp = executeWithSafeRedirects(normalized, failedUrls);
            int status = resp.statusCode();
            String contentType = resp.contentType();
            resp.url();
            String resolvedUrl = resp.url().toString();

            if (!NetUtil.isSafeHttpUrl(resolvedUrl)) {
                failedUrls.add(resolvedUrl);
                return DocumentExtractionUtils.clipTotal(fallbackSearch(input, "Unsafe final URL after redirects", failedUrls), MAX_TOTAL_OUTPUT_CHARS);
            }

            if (status >= 400) {
                failedUrls.add(resolvedUrl);
                return DocumentExtractionUtils.clipTotal(fallbackSearch(input, "HTTP " + status, failedUrls), MAX_TOTAL_OUTPUT_CHARS);
            }

            if (!isHtmlOrPlainText(contentType)) {
                failedUrls.add(resolvedUrl);
                return DocumentExtractionUtils.clipTotal(fallbackSearch(input, "Non-HTML content type", failedUrls), MAX_TOTAL_OUTPUT_CHARS);
            }

            Document doc = resp.parse();
            DocumentExtractionUtils.removeBoilerplate(doc);

            String title = DocumentExtractionUtils.safeTrim(doc.title());
            String extracted = DocumentExtractionUtils.extractMainText(doc);
            String desc = DocumentExtractionUtils.extractDescription(doc);

            if (!desc.isBlank() && extracted.length() < 400 && !extracted.contains(desc)) {
                extracted = (desc + "\n" + extracted).trim();
            }

            boolean truncated = extracted.length() > MAX_CONTENT_LENGTH;
            String bodyText = truncated ? DocumentExtractionUtils.clip(extracted, MAX_CONTENT_LENGTH) : extracted;

            StringBuilder out = new StringBuilder();
            out.append("WEBPAGE_FETCH\n");
            out.append("Status: OK\n");
            out.append("Input: ").append(input).append("\n");
            out.append("ResolvedURL: ").append(resolvedUrl).append("\n");
            out.append("HTTPStatus: ").append(status).append("\n");
            out.append("Title: ").append(title.isEmpty() ? "(none)" : title).append("\n");
            out.append("Truncated: ").append(truncated).append("\n\n");
            out.append("Content: ").append(bodyText.isEmpty() ? "(no readable text found)" : bodyText);

            if (!failedUrls.isEmpty()) {
                out.append("\n\nSome potentially relevant results could not be fetched. Failed URLs:\n");
                for (String failedUrl : failedUrls) {
                    out.append("- ").append(failedUrl).append("\n");
                }
            }

            try {
                webSearchMemoryService.saveWebSearchResult(out.toString());
            } catch (Exception e) {
                log.warn("Error saving WEBPAGE_FETCH to memory: {}", e.getMessage());
            }

            return DocumentExtractionUtils.clipTotal(out.toString(), MAX_TOTAL_OUTPUT_CHARS);

        } catch (Exception e) {
            log.error("Error fetching URL: {}, Error: {}", normalized, e.getMessage());
            failedUrls.add(normalized);
            return DocumentExtractionUtils.clipTotal(fallbackSearch(input, DocumentExtractionUtils.safeError(e.getMessage()), failedUrls), MAX_TOTAL_OUTPUT_CHARS);
        }
    }

    // ---------------------------
    // Safe redirects
    // ---------------------------

    private Connection.Response executeWithSafeRedirects(String startUrl, List<String> failedUrls) throws IOException {
        String current = startUrl;

        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            // Reject redirects that resolve to non-public destinations (SSRF defense)
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
    // Fallback search
    // ---------------------------

    private String fallbackSearch(String urlOrQuery, String reason, List<String> failedUrls) {
        String q = DocumentExtractionUtils.safeTrim(urlOrQuery);
        log.info("Fallback search. reason='{}' input='{}'", reason, q);

        String domain = extractDomain(q);
        String searchQuery;
        if (!domain.equals(q) && domain.contains(".")) {
            String hint = extractUrlHint(q);
            searchQuery = "site:" + domain + " " + (hint.isEmpty() ? domain : hint);
        } else {
            searchQuery = q;
        }

        StringBuilder output = new StringBuilder();
        output.append("WEBPAGE_FETCH\n")
                .append("Status: FALLBACK_SEARCH\n")
                .append("Reason: ").append(DocumentExtractionUtils.safeError(reason)).append("\n")
                .append("Input: ").append(q).append("\n")
                .append("SearchQuery: ").append(searchQuery).append("\n\n")
                .append("SEARCH_RESULTS\n")
                .append("Status: NOT_EXECUTED\n")
                .append("Note: DirectLinkTool is independent and does not invoke a search engine. Run your web search tool with the SearchQuery above to retrieve results.");

        // If clipping would hide the failed URLs, prefer returning them.
        if (!failedUrls.isEmpty() && output.length() > MAX_TOTAL_OUTPUT_CHARS) {
            StringBuilder failedUrlsOnly = new StringBuilder();
            failedUrlsOnly.append("WEBPAGE_FETCH\n")
                    .append("Status: FALLBACK_SEARCH\n")
                    .append("Reason: ").append(DocumentExtractionUtils.safeError(reason)).append("\n")
                    .append("Input: ").append(q).append("\n")
                    .append("SearchQuery: ").append(searchQuery).append("\n\n")
                    .append("Some potentially relevant results could not be fetched. Failed URLs (HTTP 400 or other errors):\n");
            for (String failedUrl : failedUrls) {
                failedUrlsOnly.append("- ").append(failedUrl).append("\n");
            }
            return failedUrlsOnly.toString();
        }

        if (!failedUrls.isEmpty()) {
            output.append("\n\nSome potentially relevant results could not be fetched. Failed URLs (HTTP 400 or other errors):\n");
            for (String failedUrl : failedUrls) {
                output.append("- ").append(failedUrl).append("\n");
            }
        }

        return output.toString();
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
        } catch (IllegalArgumentException ignored) {
        }

        return "https://" + v;
    }

    private String extractDomain(String urlString) {
        if (urlString == null) return null;

        try {
            URI uri = URI.create(urlString);

            if (uri.getHost() == null) {
                uri = URI.create("https://" + urlString);
            }

            String host = uri.getHost();
            if (host == null) return urlString;

            host = host.toLowerCase();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException e) {
            return urlString;
        }
    }


    private String extractUrlHint(String urlString) {
        if (urlString == null) return "";

        try {
            String normalized = normalizeUrl(urlString);

            URI uri = URI.create(normalized);

            if (uri.getScheme() == null) {
                uri = URI.create("https://" + normalized);
            }
            String path = DocumentExtractionUtils.safeTrim(uri.getPath());
            if (path.isEmpty() || path.equals("/")) return "";

            String cleaned = path
                    .replaceAll("\\.[a-zA-Z0-9]{1,5}$", "")
                    .replace('/', ' ')
                    .replace('-', ' ')
                    .trim();

            cleaned = DocumentExtractionUtils.WHITESPACE.matcher(cleaned).replaceAll(" ");
            return DocumentExtractionUtils.clip(cleaned, 80);
        } catch (IllegalArgumentException e) {
            return "";
        }
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

}
