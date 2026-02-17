package com.discord.LocalAIDiscordAgent.webSearch.utils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.regex.Pattern;


public final class DocumentExtractionUtils {

    public static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public static String extractMainText(Document doc) {
        if (doc == null) return "";

        String text = "";
        Element container = doc.selectFirst("article");
        if (container != null) text = normalizeWhitespace(container.text());

        if (text.length() < 400) {
            container = doc.selectFirst("main");
            if (container != null) text = normalizeWhitespace(container.text());
        }

        if (text.length() < 400) {
            container = doc.body();
            text = normalizeWhitespace(container.text());
        }

        return text;
    }

    public static String extractDescription(Document doc) {
        if (doc == null) return "";

        Element d1 = doc.selectFirst("meta[name=description]");
        if (d1 != null) {
            String v = safeTrim(d1.attr("content"));
            if (!v.isEmpty()) return v;
        }

        Element d2 = doc.selectFirst("meta[property=og:description]");
        if (d2 != null) {
            String v = safeTrim(d2.attr("content"));
            if (!v.isEmpty()) return v;
        }

        return "";
    }

    public static void removeBoilerplate(Document doc) {
        if (doc == null) return;

        doc.select("script, style, noscript, iframe, svg, canvas, form, nav, header, footer, aside").remove();
        doc.select(
                "[id*=cookie i], [class*=cookie i], " +
                        "[id*=consent i], [class*=consent i], " +
                        "[id*=subscribe i], [class*=subscribe i], " +
                        "[id*=newsletter i], [class*=newsletter i], " +
                        "[id*=modal i], [class*=modal i], " +
                        "[id*=popup i], [class*=popup i], " +
                        "[id*=banner i], [class*=banner i], " +
                        "[id*=advert i], [class*=advert i], " +
                        "[id*=ads i], [class*=ads i], " +
                        "div[role=dialog], div[aria-modal=true]"
        ).remove();
    }


    public static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    public static String normalizeWhitespace(String s) {
        if (s == null) return "";
        return WHITESPACE.matcher(s).replaceAll(" ").trim();
    }

    public static void safeAdd(List<String> list, String value) {
        String v = safeTrim(value);
        if (!v.isEmpty()) list.add(v);
    }

    public static String safeError(String msg) {
        if (msg == null || msg.isBlank()) return "unknown";
        return normalizeWhitespace(msg);
    }

    public static String clipTotal(String s, int MAX_TOTAL_OUTPUT_CHARS) {
        return clip(s, MAX_TOTAL_OUTPUT_CHARS);
    }

    public static String clip(String s, int maxChars) {
        if (s == null) return "";
        if (maxChars <= 0) return "";
        String t = s.trim();
        if (t.length() <= maxChars) return t;
        return t.substring(0, maxChars) + "...";
    }

}
