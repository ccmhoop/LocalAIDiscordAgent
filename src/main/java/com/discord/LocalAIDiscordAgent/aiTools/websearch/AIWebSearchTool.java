package com.discord.LocalAIDiscordAgent.aiTools.websearch;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

@Slf4j
@Component
public class AIWebSearchTool {

    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_TEXT_LENGTH = 15_000;


    @Tool(
            description = """
        Fetch and return ONLY page-specific information from the given URL.
        Excludes navigation, menus, headers, footers, and site-wide boilerplate.
        Must be used before answering any question about webpage content.
        """
    )
    public String webSearch(
            @ToolParam(description = "Absolute HTTP or HTTPS URL") String url
    ) {

        if (!isValidUrl(url)) {
            return "Invalid URL: " + url;
        }

        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; AIWebSearchTool/1.0)")
                    .timeout(TIMEOUT_MS)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .method(Connection.Method.GET)
                    .get();

            removeBoilerplate(document);

            Element mainContent = selectMainContent(document)
                    .orElse(document.body());

            String title = document.title();
            String description = document
                    .selectFirst("meta[name=description]")
                    != null
                    ? document.selectFirst("meta[name=description]").attr("content")
                    : "";

            String contentText = normalizeWhitespace(mainContent.text());

//            if (contentText.isBlank()) {
//                return "No page-specific content found at: " + url;
//            }

            StringBuilder result = new StringBuilder();

//            if (!title.isBlank()) {
//                result.append("Title: ").append(title).append("\n\n");
//            }
//
//            if (!description.isBlank()) {
//                result.append("Description: ").append(description).append("\n\n");
//            }
//
//            if (contentText.length() > MAX_TEXT_LENGTH) {
//                contentText = contentText.substring(0, MAX_TEXT_LENGTH)
//                        + "\n\n[Content truncated]";
//            }

            result.append("Content:\n").append(contentText);

            System.out.println(result.toString());
            return result.toString();

        } catch (IOException e) {
            log.warn("Web search failed for URL: {}", url, e);
            return "Failed to retrieve content from: " + url;
        }
    }

    private Optional<Element> selectMainContent(Document document) {
        return Optional.ofNullable(
                document.selectFirst(
                        "main, article, [role=main], #content, .content, .main-content"
                )
        );
    }

    private void removeBoilerplate(Document document) {
        document.select(
                """
                script, style, noscript, iframe, svg, canvas,
                nav, aside, footer, header,
                .nav, .navigation, .menu, .sidebar,
                .footer, .header, .site-header, .site-footer,
                .breadcrumb, .breadcrumbs, .toc, .table-of-contents,
                .infobox, .metadata, .mw-indicators
                """
        ).remove();
    }

    private boolean isValidUrl(String url) {
        try {
            URL parsed = new URL(url);
            return "http".equalsIgnoreCase(parsed.getProtocol())
                    || "https".equalsIgnoreCase(parsed.getProtocol());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private String normalizeWhitespace(String input) {
        return input.replaceAll("\\s+", " ").trim();
    }
}
