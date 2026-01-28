package com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AIWebFilterTool {

    private static final int MAX_OUTPUT_CHARS = 1800;
    private static final int MAX_SENTENCES = 10;
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");
    private static final Pattern WORD = Pattern.compile("[a-zA-Z0-9]{3,}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "the","and","for","with","that","this","from","into","your","you","are","was","were","have","has","had",
            "but","not","can","could","should","would","will","their","they","them","his","her","she","him","its","our",
            "about","what","when","where","which","who","why","how","also","than","then","there","here","over","under",
            "use","using","used","more","most","some","such","only","just","like","may","might","must","shall"
    ));

    @Tool(
            description = """
                    Filter the output of webSearch to the smallest excerpt needed to answer the current question.
                    The 'pageText' parameter MUST be the exact output returned by webSearch.
                   
                    Call AFTER webSearch when you need details from the fetched page.
                    """
    )
    public String webFilterText(
            @ToolParam(description = "Text previously retrieved from webSearch") String pageText,
            @ToolParam(description = "User question or information asked about") String question
    ) {
        String q = safeTrim(question);
        String text = safeTrim(pageText);
        if (text.isEmpty()) {
            return "WEBPAGE_FILTER\nStatus: ERROR\nError: Empty pageText.";
        }
        if (q.isEmpty()) {
            return clip("WEBPAGE_FILTER\nStatus: ERROR\nError: Empty question.\n\nInput:\n" + text, MAX_OUTPUT_CHARS);
        }

        String content = extractContentSection(text);
        if (content.isEmpty()) {
            content = text;
        }

        List<String> keywords = extractKeywords(q);
        if (keywords.isEmpty()) {
            return clip("WEBPAGE_FILTER\nStatus: NO_KEYWORDS\nQuestion: " + q + "\n\nContent:\n" + content, MAX_OUTPUT_CHARS);
        }

        List<ScoredSentence> scored = scoreSentences(content, keywords);
        scored.sort(Comparator.comparingInt(ScoredSentence::score).reversed());

        List<ScoredSentence> top = new ArrayList<>();
        for (ScoredSentence s : scored) {
            if (s.score() <= 0) break;
            top.add(s);
            if (top.size() >= MAX_SENTENCES) break;
        }

        if (top.isEmpty()) {
            return clip(
                    "WEBPAGE_FILTER\nStatus: NO_MATCH\nQuestion: " + q + "\nKeywords: " + String.join(", ", keywords) +
                            "\n\nMessage: No directly relevant text found in the fetched page content.",
                    MAX_OUTPUT_CHARS
            );
        }

        // Re-sort by original order so the excerpt reads coherently.
        top.sort(Comparator.comparingInt(ScoredSentence::index));

        StringBuilder out = new StringBuilder();
        out.append("WEBPAGE_FILTER\n");
        out.append("Status: OK\n");
        out.append("Question: ").append(q).append("\n");
        out.append("Keywords: ").append(String.join(", ", keywords)).append("\n\n");
        out.append("Relevant excerpt:\n");

        for (ScoredSentence s : top) {
            out.append("- ").append(s.sentence()).append("\n");
        }

        return clip(out.toString(), MAX_OUTPUT_CHARS);
    }

    private String extractContentSection(String pageText) {
        // Expected upstream format: ... "Content: <text>". Keep it robust.
        int idx = pageText.indexOf("Content:");
        if (idx < 0) return "";
        return safeTrim(pageText.substring(idx + "Content:".length()));
    }

    private List<String> extractKeywords(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        java.util.regex.Matcher m = WORD.matcher(q);
        List<String> words = new ArrayList<>();
        while (m.find()) {
            String w = m.group();
            if (STOPWORDS.contains(w)) continue;
            words.add(w);
        }

        // Prefer longer/more distinctive words; de-dup while preserving preference.
        words.sort(Comparator.<String>comparingInt(String::length).reversed());
        List<String> out = new ArrayList<>();
        for (String w : words) {
            if (!out.contains(w)) out.add(w);
            if (out.size() >= 12) break;
        }
        return out;
    }

    private List<ScoredSentence> scoreSentences(String content, List<String> keywords) {
        String normalized = normalizeWhitespace(content);
        if (normalized.isEmpty()) return List.of();

        String[] sentences = SENTENCE_SPLIT.split(normalized);
        List<ScoredSentence> scored = new ArrayList<>(sentences.length);
        for (int i = 0; i < sentences.length; i++) {
            String s = safeTrim(sentences[i]);
            if (s.isEmpty()) continue;
            String lower = s.toLowerCase(Locale.ROOT);

            int score = 0;
            for (String k : keywords) {
                if (lower.contains(k)) score += 1;
            }

            if (score > 0) {
                int len = s.length();
                if (len >= 80 && len <= 280) score += 1;
            }

            scored.add(new ScoredSentence(i, score, s));
        }
        return scored;
    }

    private String normalizeWhitespace(String s) {
        if (s == null) return "";
        return WHITESPACE.matcher(s).replaceAll(" ").trim();
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private String clip(String s, int maxChars) {
        if (s == null) return "";
        if (maxChars <= 0) return "";
        String t = s.trim();
        if (t.length() <= maxChars) return t;
        return t.substring(0, maxChars) + "…";
    }

    private record ScoredSentence(int index, int score, String sentence) {}
}