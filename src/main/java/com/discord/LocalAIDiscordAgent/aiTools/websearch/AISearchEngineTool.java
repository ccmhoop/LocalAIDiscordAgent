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
    private static final int MAX_EXCERPT_CHARS = 2000;

    @Tool(description = """
            Search the web using DuckDuckGo (HTML endpoint) and return the top 3 results
            PLUS extracted content from each of the top 3 pages (excerpt).
            Each result includes title, URL, snippet, and page excerpt.
            """)
    public String searchAndFetch(@ToolParam(description = "The search query to look up on the web") String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String searchUrl = "https://html.duckduckgo.com/html/?q=" + encoded;

        StringBuilder out = new StringBuilder();
        out.append("Search results for: ").append(query).append("\n\n");

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
                out.append("No results found for your query.");
                return out.toString();
            }

            for (int i = 0; i < top.size(); i++) {
                SearchResult sr = top.get(i);

                out.append("Result ").append(i + 1).append(":\n")
                        .append("Title: ").append(sr.title).append("\n")
                        .append("URL: ").append(sr.url).append("\n")
                        .append("Description: ").append(sr.snippet).append("\n");

                String pageInfo = fetchAndExtractPage(sr.url);
                out.append("Page extract: ").append(pageInfo).append("\n\n");
            }

            return out.toString();

        } catch (IOException e) {
            log.error("Search error: {}", e.getMessage(), e);
            return out.append("Error: Unable to perform search. ").append(e.getMessage()).toString();
        }
    }

    private String fetchAndExtractPage(String url) {
        if (!isSafeHttpUrl(url)) {
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
                    .execute();

            String contentType = resp.contentType();
            if (contentType == null || !contentType.toLowerCase().contains("text/html")) {
                return "[Skipped: non-HTML content type: " + (contentType == null ? "unknown" : contentType) + "]";
            }

            Document doc = resp.parse();
            String text = extractMainText(doc);

            if (text.isBlank()) {
                return "[No readable text found]";
            }

            if (text.length() > MAX_EXCERPT_CHARS) {
                text = text.substring(0, MAX_EXCERPT_CHARS) + "…";
            }
            return text;

        } catch (IOException e) {
            log.warn("Failed to fetch page {}: {}", url, e.getMessage());
            return "[Failed to fetch page: " + e.getMessage() + "]";
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
