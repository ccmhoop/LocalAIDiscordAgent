package com.discord.LocalAIDiscordAgent.aiTools.websearch;

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
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AISearchEngineTool {

    private static final Pattern UDDG_PARAM = Pattern.compile("[?&]uddg=([^&]+)");

    private static final int SEARCH_TIMEOUT_MS = 15000;
    private static final int PAGE_TIMEOUT_MS = 15000;
    private static final int MAX_RESULTS = 3;
    private static final int MAX_EXCERPT_CHARS = 1200;
    private static final int MAX_TOTAL_OUTPUT_CHARS = 9000;

    private final List<String> failedUrls = new ArrayList<>();


    @Tool(description = """
        Web search via DuckDuckGo HTML.
        Returns up to 3 results (title, resolved URL, snippet) and a short page excerpt for each.
        Use when facts may be outdated or when a URL fetch fails.
        """)
    public String searchAndFetch(
            @ToolParam(description = "Search query. Use only the exact query string; do not add prior conversation context.")
            String query
    ) {
        failedUrls.clear();

        String safeQuery = (query == null) ? "" : query.trim();
        if (safeQuery.isEmpty()) {
            return "SEARCH_RESULTS\nStatus: ERROR\nError: Empty query.";
        }

        String encoded = URLEncoder.encode(safeQuery, StandardCharsets.UTF_8);
        String searchUrl = "https://html.duckduckgo.com/html/?q=" + encoded;

        StringBuilder out = new StringBuilder();
        out.append("SEARCH_RESULTS\n");
        out.append("Query: ").append(safeQuery).append("\n");
        out.append("Status: OK\n\n");

        try {
            Document doc = Jsoup.connect(searchUrl)
                    .timeout(SEARCH_TIMEOUT_MS)
                    .followRedirects(true)
                    .userAgent(commonUserAgent())
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .referrer("https://duckduckgo.com/")
                    .get();

            Elements resultBlocks = doc.select("div.results > div.result, div.result");
            log.info("DDG result blocks found: {}", resultBlocks.size());

            List<SearchResult> top = new ArrayList<>();
            for (Element r : resultBlocks) {
                if (top.size() >= MAX_RESULTS) break;

                Element a = r.selectFirst("a.result__a");
                if (a == null) a = r.selectFirst("h2 a, a[href]");
                if (a == null) continue;

                String title = a.text().trim();
                if (title.isEmpty()) continue;

                String rawHref = a.attr("href");
                String url = normalizeDuckDuckGoUrl(rawHref);

                Element snip = r.selectFirst(".result__snippet, .result__snippet--expanded, .result__body");
                String snippet = (snip != null) ? snip.text().trim() : "No description available";

                top.add(new SearchResult(title, url, snippet));
            }

            if (top.isEmpty()) {
                out.append("Results: 0\nMessage: No results found.");
                return clipTotal(out.toString());
            }

            out.append("Results: ").append(top.size()).append("\n\n");

            for (int i = 0; i < top.size(); i++) {
                SearchResult sr = top.get(i);

                out.append("---\n");
                out.append("Result ").append(i + 1).append("\n")
                        .append("Title: ").append(sr.title).append("\n")
                        .append("URL: ").append(sr.url).append("\n")
                        .append("Snippet: ").append(sr.snippet).append("\n");

                String pageInfo = fetchAndExtractPage(sr.url);
                out.append("Excerpt: ").append(pageInfo).append("\n");
            }

            System.out.println(out);

            if (!failedUrls.isEmpty()) {
                StringBuilder failedUrlsOutput = new StringBuilder();
                failedUrlsOutput.append("SEARCH_RESULTS\n");
                failedUrlsOutput.append("Query: ").append(safeQuery).append("\n");
                failedUrlsOutput.append("Status: OK\n\n");
                failedUrlsOutput.append("Some potentially relevant results could not be fetched. Failed URLs (HTTP 400 or other errors):\n");
                for (String failedUrl : failedUrls) {
                    failedUrlsOutput.append("- ").append(failedUrl).append("\n");
                }

                if (out.length() > MAX_TOTAL_OUTPUT_CHARS) {
                    return clipTotal(failedUrlsOutput.toString());
                }

                out.append("\nSome potentially relevant results could not be fetched. Failed URLs:\n");
                for (String failedUrl : failedUrls) {
                    out.append("- ").append(failedUrl).append("\n");
                }
            }

            return clipTotal(out.toString());

        } catch (IOException e) {
            log.error("Search error: {}", e.getMessage(), e);
            return clipTotal(out.append("\nStatus: ERROR\nError: Unable to perform search. ")
                    .append(safeError(e.getMessage()))
                    .toString());
        }
    }

    private String fetchAndExtractPage(String url) {
        if (!isSafeHttpUrl(url)) {
            failedUrls.add(url);
            return "[Skipped: unsafe or non-http(s) URL]";
        }

        try {
            Connection.Response resp = Jsoup.connect(url)
                    .timeout(PAGE_TIMEOUT_MS)
                    .followRedirects(true)
                    .maxBodySize(2_000_000)
                    .userAgent(commonUserAgent())
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .ignoreHttpErrors(true)
                    .execute();

            int status = resp.statusCode();
            if (status >= 400) {
                failedUrls.add(url);
                return "[Failed to fetch page: HTTP " + status + "]";
            }

            String contentType = resp.contentType();
            if (contentType == null || !contentType.toLowerCase().contains("text/html")) {
                failedUrls.add(url);
                return "[Skipped: non-HTML content type: " + (contentType == null ? "unknown" : contentType) + "]";
            }

            Document doc = resp.parse();
            String text = extractMainText(doc);

            if (text.isBlank()) {
                failedUrls.add(url);
                return "[No readable text found]";
            }

            return clip(text, MAX_EXCERPT_CHARS);

        } catch (IOException e) {
            log.warn("Failed to fetch page {}: {}", url, e.getMessage());
            failedUrls.add(url);
            return "[Failed to fetch page: " + safeError(e.getMessage()) + "]";
        }
    }


    private String extractMainText(Document doc) {
        doc.select("script, style, noscript, iframe, svg, canvas, form, nav, header, footer, aside").remove();

        Element container = doc.selectFirst("article");
        if (container == null) container = doc.selectFirst("main");
        if (container == null) container = doc.body();

        String text = container.text();
        return normalizeWhitespace(text);
    }

    private String normalizeWhitespace(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }

    /**
     * DuckDuckGo sometimes returns redirect URLs like:
     *   https://duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com
     */
    private String normalizeDuckDuckGoUrl(String href) {
        if (href == null || href.isBlank()) return "No URL available";

        String absolute = href.startsWith("/") ? "https://duckduckgo.com" + href : href;

        Matcher m = UDDG_PARAM.matcher(absolute);
        if (m.find()) {
            String encodedTarget = m.group(1);
            try {
                return URLDecoder.decode(encodedTarget, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
        return absolute;
    }

    private String commonUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
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
        // keep tool output stable and one-line to avoid downstream parsing issues
        return msg.replaceAll("\\s+", " ").trim();
    }

    /**
     * Basic SSRF mitigation:
     * - only http/https
     * - reject localhost / loopback / private / link-local addresses
     * Note: This is a baseline; you can further harden with allowlists, DNS pinning, etc.
     */
    private boolean isSafeHttpUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            scheme = scheme.toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) return false;

            String host = uri.getHost();
            if (host == null || host.isBlank()) return false;

            String h = host.toLowerCase();
            if (h.equals("localhost") || h.endsWith(".local")) return false;

            InetAddress addr = InetAddress.getByName(host);
            return !(addr.isAnyLocalAddress()
                    || addr.isLoopbackAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isMulticastAddress());

        } catch (Exception e) {
            return false;
        }
    }

    private record SearchResult(String title, String url, String snippet) {}
}
