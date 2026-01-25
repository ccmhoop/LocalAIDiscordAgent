package com.discord.LocalAIDiscordAgent.aiTools.websearch;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.concurrent.*;

@Slf4j
@Component
public class AIWebSearchTool {

    private static final int TIMEOUT_MS = 12000;
    private static final int MAX_BODY_BYTES = 1_000_000;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_TOTAL_OUTPUT_CHARS = 6000;

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Set<String> NON_HTML_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/zip",
            "application/octet-stream",
            "image/",
            "audio/",
            "video/"
    );

    /**
     * Optional dependency.
     * If a qualified bean named "webSearchEngine" is not available, the tool will still run URL fetches,
     * but fallback-to-search will return a bounded error message instead of throwing.
     */
    @Autowired(required = false)
    @Qualifier("webSearchEngine")
    private AISearchEngineTool searchEngineTool;


    @Tool(description = """
            Fetch readable text from a single HTTP/HTTPS URL.
            Returns title + bounded content excerpt.
            If the input is not a valid/safe URL or the page cannot be fetched, falls back to web search.
            """)
    public String webSearch(@ToolParam(description = "Absolute HTTP/HTTPS URL to fetch. If the value is not a valid URL, the tool will treat it as a search query and fall back to web search.") String url) {
        String input = safeTrim(url);
        if (input.isEmpty()) {
            return "WEBPAGE_FETCH\nStatus: ERROR\nError: Empty URL/query.";
        }

        // If this doesn't look like a URL (spaces, no dots, etc.), treat it as a query.
        if (looksLikeQuery(input)) {
            return clipTotal(fallbackSearch(input, "Input does not look like a URL"));
        }

        String normalized = normalizeUrl(input);
        if (!isValidUrl(normalized) || !isSafeHttpUrl(normalized)) {
            log.warn("Invalid or unsafe URL: {} (normalized: {})", input, normalized);
            return clipTotal(fallbackSearch(input, "Invalid or unsafe URL"));
        }

        try {
            Connection connection = Jsoup.connect(normalized)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .maxBodySize(MAX_BODY_BYTES)
                    .userAgent(commonUserAgent())
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .ignoreHttpErrors(true);

            Connection.Response resp = executeWithTimeout(connection::execute, TIMEOUT_MS);
            if (resp == null) {
                log.warn("Timeout or null response when fetching: {}", normalized);
                return clipTotal(fallbackSearch(input, "Timeout while fetching URL"));
            }

            int status = resp.statusCode();
            String contentType = resp.contentType();
            resp.url();
            String resolvedUrl = resp.url().toString();

            if (status >= 400) {
                log.warn("HTTP {} for URL {}", status, resolvedUrl);
                return clipTotal(fallbackSearch(input, "HTTP " + status));
            }

            if (!isHtml(contentType)) {
                log.warn("Non-HTML content type for {}: {}", resolvedUrl, contentType);
                return clipTotal(fallbackSearch(input, "Non-HTML content type"));
            }

            Document doc = resp.parse();

            // Clean document by removing scripts, styles, etc.
            doc.select("script, style, noscript, iframe, svg, canvas, form, nav, header, footer, aside").remove();

            String title = safeTrim(doc.title());
            String extracted = extractMainText(doc);
            boolean truncated = extracted.length() > MAX_CONTENT_LENGTH;
            String bodyText = truncated ? clip(extracted, MAX_CONTENT_LENGTH) : extracted;

            StringBuilder out = new StringBuilder();
            out.append("WEBPAGE_FETCH\n");
            out.append("Status: OK\n");
            out.append("Input: ").append(input).append("\n");
            out.append("ResolvedURL: ").append(resolvedUrl).append("\n");
            out.append("HTTPStatus: ").append(status).append("\n");
            out.append("Title: ").append(title.isEmpty() ? "(none)" : title).append("\n");
            out.append("Truncated: ").append(truncated).append("\n\n");
            out.append("Content: ").append(bodyText.isEmpty() ? "(no readable text found)" : bodyText);
            return clipTotal(out.toString());

        } catch (Exception e) {
            log.error("Error fetching URL: {}, Error: {}", normalized, e.getMessage());
            return clipTotal(fallbackSearch(input, safeError(e.getMessage())));
        }
    }

    private String fallbackSearch(String urlOrQuery, String reason) {
        String q = safeTrim(urlOrQuery);
        log.info("Fallback search. reason='{}' input='{}'", reason, q);

        String domain = extractDomain(q);
        String searchQuery;
        if (!domain.equals(q) && domain.contains(".")) {
            String hint = extractUrlHint(q);
            searchQuery = "site:" + domain + " " + (hint.isEmpty() ? domain : hint);
        } else {
            searchQuery = q;
        }

        if (searchEngineTool == null) {
            return "WEBPAGE_FETCH\n" +
                    "Status: FALLBACK_SEARCH\n" +
                    "Reason: " + safeError(reason) + "\n" +
                    "Input: " + q + "\n" +
                    "SearchQuery: " + searchQuery + "\n\n" +
                    "SEARCH_RESULTS\nStatus: ERROR\nError: Search engine tool bean not available (expected bean name: webSearchEngine).";
        }

        String results = searchEngineTool.searchAndFetch(searchQuery);
        return "WEBPAGE_FETCH\n" +
                "Status: FALLBACK_SEARCH\n" +
                "Reason: " + safeError(reason) + "\n" +
                "Input: " + q + "\n" +
                "SearchQuery: " + searchQuery + "\n\n" +
                results;
    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private String extractDomain(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            // If we can't parse it as a URL, just return the original string
            return urlString;
        }
    }

    private <T> T executeWithTimeout(Callable<T> callable, long timeoutMs) {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<T> future = executor.submit(callable);
            try {
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("Execution timed out after {}ms", timeoutMs);
                return null;
            } catch (Exception e) {
                future.cancel(true);
                log.warn("Execution failed: {}", e.getMessage());
                return null;
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private String extractMainText(Document doc) {
        Element container = doc.selectFirst("article");
        if (container == null) container = doc.selectFirst("main");
        if (container == null) {
            container = doc.body();
        }
        if (container == null) return "";
        return normalizeWhitespace(container.text());
    }

    private boolean looksLikeQuery(String input) {
        if (input == null) return true;
        if (input.chars().anyMatch(Character::isWhitespace)) return true;
        return !input.contains(".");
    }

    private String normalizeUrl(String raw) {
        String v = safeTrim(raw);
        if (v.startsWith("http://") || v.startsWith("https://")) return v;
        return "https://" + v;
    }

    private boolean isHtml(String contentType) {
        if (contentType == null || contentType.isBlank()) return true;
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("text/html")) return true;

        for (String bad : NON_HTML_CONTENT_TYPES) {
            if (bad.endsWith("/") && ct.startsWith(bad)) return false;
            if (!bad.endsWith("/") && ct.startsWith(bad)) return false;
        }
        return false;
    }

    /**
     * Baseline SSRF mitigation: only http/https and reject localhost/private/link-local.
     * Mirrors the approach in {@link AISearchEngineTool} to keep behavior consistent.
     */
    private boolean isSafeHttpUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) return false;

            String host = uri.getHost();
            if (host == null || host.isBlank()) return false;

            String h = host.toLowerCase(Locale.ROOT);
            if (h.equals("localhost") || h.endsWith(".local")) return false;

            java.net.InetAddress addr = java.net.InetAddress.getByName(host);
            return !(addr.isAnyLocalAddress()
                    || addr.isLoopbackAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isMulticastAddress());
        } catch (Exception e) {
            return false;
        }
    }

    private String extractUrlHint(String urlString) {
        try {
            URL u = new URL(normalizeUrl(urlString));
            String path = safeTrim(u.getPath());
            if (path.isEmpty() || path.equals("/")) return "";
            String cleaned = path
                    .replaceAll("\\.[a-zA-Z0-9]{1,5}$", "")
                    .replace('/', ' ')
                    .replace('-', ' ')
                    .trim();
            cleaned = WHITESPACE.matcher(cleaned).replaceAll(" ");
            return clip(cleaned, 80);
        } catch (Exception e) {
            return "";
        }
    }

    private String commonUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    }

    private String normalizeWhitespace(String s) {
        if (s == null) return "";
        return WHITESPACE.matcher(s).replaceAll(" ").trim();
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private String clipTotal(String s) {
        return clip(s, MAX_TOTAL_OUTPUT_CHARS);
    }

    private String clip(String s, int maxChars) {
        if (s == null) return "";
        if (maxChars <= 0) return "";
        String t = s.trim();
        if (t.length() <= maxChars) return t;
        return t.substring(0, maxChars) + "…";
    }

    private String safeError(String msg) {
        if (msg == null || msg.isBlank()) return "unknown";
        return normalizeWhitespace(msg);
    }
}
