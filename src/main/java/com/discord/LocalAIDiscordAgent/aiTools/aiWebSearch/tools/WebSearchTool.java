package com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.tools;

import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.service.WebSearchMemoryService;
import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.utils.DocumentExtractionUtils;
import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.utils.NetUtil;
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

@Slf4j
@Component
public class WebSearchTool {

    private static final Pattern UDDG_PARAM = Pattern.compile("[?&]uddg=([^&]+)");
    private static final Pattern U3_PARAM = Pattern.compile("[?&]u3=([^&]+)");

    private static final int SEARCH_TIMEOUT_MS = 15_000;
    private static final int PAGE_TIMEOUT_MS = 15_000;
    private static final int MAX_RESULTS = 3;
    private static final int MAX_EXCERPT_CHARS = 1_200;
    private static final int MAX_TOTAL_OUTPUT_CHARS = 9_000;
    private static final int MAX_REDIRECTS = 5;

    private final WebSearchMemoryService webSearchMemoryService;

    public WebSearchTool(WebSearchMemoryService webSearchMemoryService) {
        this.webSearchMemoryService = webSearchMemoryService;
    }

    @Tool(
            name = "web_search",
            description = """
                    Live web search via DuckDuckGo HTML.
                    
                    Refine the query to improve relevance.
                    """)
    public String webSearch(
            @ToolParam(description = "Search query to look up. Refine the query to improve relevance.")
            String query
    ) {
        String safeQuery = DocumentExtractionUtils.safeTrim(query);
        if (safeQuery.isEmpty()) {
            return "SEARCH_RESULTS\nStatus: ERROR\nError: Empty query.";
        }

        final List<String> failedUrls = new ArrayList<>();

        // 1) Live search
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

                // Skip DDG ad/tracking JS wrapper links (common cause of "not fetching sites")
                if (rawHref.startsWith("/y.js") || rawHref.contains("duckduckgo.com/y.js")) continue;

                String url = normalizeDuckDuckGoUrl(rawHref);
                if (DocumentExtractionUtils.safeTrim(url).isEmpty()) continue;

                Element snip = r.selectFirst(".result__snippet, .result__snippet--expanded, .result__body");
                String snippet = snip != null ? DocumentExtractionUtils.safeTrim(snip.text()) : "No description available";

                top.add(new SearchResult(title, url, snippet));
            }

            if (top.isEmpty()) {
                return "SEARCH_RESULTS\nQuery: " + safeQuery + "\nStatus: OK\n\nResults: 0\nMessage: No results found.";
            }

            StringBuilder out = new StringBuilder();
            out.append("SEARCH_RESULTS\n");
            out.append("Query: ").append(safeQuery).append("\n");
            out.append("Status: OK\n\n");
            out.append("Results: ").append(top.size()).append("\n\n");

            for (int i = 0; i < top.size(); i++) {
                SearchResult sr = top.get(i);
                out.append("---\n");
                out.append("Result ").append(i + 1).append("\n");
                out.append("Title: ").append(sr.title).append("\n");
                out.append("URL: ").append(sr.url).append("\n");
                out.append("Snippet: ").append(sr.snippet).append("\n");

                String excerpt = fetchAndExtractPage(sr.url, failedUrls);
                out.append("Excerpt: ").append(excerpt).append("\n");
            }

            if (!failedUrls.isEmpty()) {
                out.append("\nSome potentially relevant results could not be fetched. Failed URLs:\n");
                for (String u : failedUrls) out.append("- ").append(u).append("\n");
            }

            String result = DocumentExtractionUtils.clipTotal(out.toString(), MAX_TOTAL_OUTPUT_CHARS);

            try {
                webSearchMemoryService.saveWebSearchResult(result);
            } catch (Exception e) {
                log.warn("Failed to save SEARCH_RESULTS to memory: {}", e.getMessage());
            }

            return result;

        } catch (IOException e) {
            return DocumentExtractionUtils.clipTotal(
                    "SEARCH_RESULTS\nQuery: " + safeQuery + "\nStatus: ERROR\nError: Unable to perform search. "
                            + DocumentExtractionUtils.safeError(e.getMessage()),
                    MAX_TOTAL_OUTPUT_CHARS
            );
        }
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

            if (status >= 400) {
                DocumentExtractionUtils.safeAdd(failedUrls, resolvedUrl);
                return "[Failed to fetch page: HTTP " + status + "]";
            }

            // handle text/plain quickly
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

            // parse HTML/XHTML
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

    // ---------- Redirects (validated each hop) ----------
    private Connection.Response executeWithSafeRedirects(String startUrl, List<String> failedUrls) throws IOException {
        String current = startUrl;

        for (int i = 0; i <= MAX_REDIRECTS; i++) {

            // ✅ Correct: only block if NOT safe
            if (!NetUtil.isSafeHttpUrl(current)) {
                DocumentExtractionUtils.safeAdd(failedUrls, current);
                throw new IOException("Unsafe URL (SSRF blocked): " + current);
            }

            Connection.Response resp = Jsoup.connect(current)
                    .timeout(PAGE_TIMEOUT_MS)
                    .followRedirects(false)
                    .maxBodySize(2_000_000)
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


    // ---------- DDG URL normalization ----------
    private String normalizeDuckDuckGoUrl(String href) {
        if (href == null) return "";
        String h = href.trim();
        if (h.isEmpty()) return "";

        // Drop DDG ad wrapper if it sneaks through
        if (h.startsWith("/y.js") || h.contains("duckduckgo.com/y.js")) return "";

        // Fix malformed DDG hrefs like "/duckduckgo.com/l/?uddg=..."
        if (h.startsWith("/duckduckgo.com/")) {
            h = h.substring("/duckduckgo.com".length()); // => "/l/?uddg=..."
        }

        // Make absolute
        String absolute;
        if (h.startsWith("//")) {
            absolute = "https:" + h; // scheme-relative
        } else if (h.startsWith("/")) {
            absolute = "https://duckduckgo.com" + h;
        } else {
            absolute = h;
        }

        // Unwrap DDG organic redirect: ?uddg=<urlencoded target>
        Matcher uddg = UDDG_PARAM.matcher(absolute);
        if (uddg.find()) {
            try {
                return URLDecoder.decode(uddg.group(1), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }

        // Unwrap DDG ad click wrapper: /y.js?...&u3=<urlencoded target> (kept for completeness)
        Matcher u3 = U3_PARAM.matcher(absolute);
        if (u3.find()) {
            try {
                return URLDecoder.decode(u3.group(1), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }

        return absolute;
    }

    private record SearchResult(String title, String url, String snippet) {
    }
}
