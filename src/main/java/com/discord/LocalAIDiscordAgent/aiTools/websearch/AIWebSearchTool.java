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

@Slf4j
@Component
public class AIWebSearchTool {


    @Tool(description = """
            Fetch webpage text from a URL for downstream filtering.
            Return a structured result containing: status, final_url, title, description, and pageText.
            Exclude scripts/styles and common boilerplate where possible.
            Focus only on the content of the current URL, not on previous conversation context.
            """)
    public String webSearch(@ToolParam(description = "Absolute HTTP or HTTPS URL. Process only this URL without considering previous conversation context.") String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (compatible; SpringAI-Bot/1.0)")
                    .get();
            String title = doc.title();
            String bodyText = doc.body().text();

            if (bodyText.length() > 2000) {
                bodyText = bodyText.substring(0, 2000) + "...";
            }

            return String.format("Title: %s\n\nContent: %s", title, bodyText);
        } catch (IOException e) {
            return "Error: Unable to fetch content.";
        }
    }

}
