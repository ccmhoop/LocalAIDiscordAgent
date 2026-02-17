package com.discord.LocalAIDiscordAgent.chatMemory.toolChatMemory.encoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatMessageContentEncoder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // last-resort parse if ToolResponse is only visible via toString()
    private static final Pattern RESPONSE_DATA_IN_TOSTRING =
            Pattern.compile("responseData=([^\\]]+)]");

    private ChatMessageContentEncoder() {}

    public static String encodeForDb(Message message) {
        if (message == null) return "";

        // Skip SYSTEM completely (you don't want it in DB)
        if (message instanceof SystemMessage) return "";

        // Skip assistant tool-call request messages (the empty assistant with toolCalls)
        if (message instanceof AssistantMessage am) {
            if (am.getToolCalls() != null && !am.getToolCalls().isEmpty()) {
                return "";
            }
        }

        // Normal text messages (USER/ASSISTANT)
        String text = safeText(message);
        if (!text.isBlank()) return text;

        // TOOL: store only tool output (responseData) as JSON
        if (message instanceof ToolResponseMessage trm) {
            return encodeToolResponse(trm);
        }

        return "";
    }

    private static String encodeToolResponse(ToolResponseMessage trm) {
        List<?> responses = trm.getResponses();
        if (responses == null || responses.isEmpty()) return "";

        // Normalize each responseData to an Object (Map/List/String) so arrays don't get double-quoted
        List<Object> normalized = new ArrayList<>();

        for (Object r : responses) {
            Object rd = extractResponseData(r);
            Object obj = normalizeResponseData(rd);
            if (obj == null) continue;

            if (obj instanceof String s && s.isBlank()) continue;
            normalized.add(obj);
        }

        if (normalized.isEmpty()) return "";

        // If only one response, return it as JSON/text
        if (normalized.size() == 1) {
            return toJsonText(normalized.get(0));
        }

        // If multiple tool responses, store a JSON array
        return toJson(normalized);
    }

    /**
     * Extract responseData across Spring AI versions:
     * - getResponseData()
     * - responseData()   (record accessor)
     * - field 'responseData'
     * - fallback parse from toString()
     */
    private static Object extractResponseData(Object toolResponse) {
        if (toolResponse == null) return null;

        Object v = invoke(toolResponse, "getResponseData");
        if (v != null) return v;

        v = invoke(toolResponse, "responseData");
        if (v != null) return v;

        v = field(toolResponse, "responseData");
        if (v != null) return v;

        // last resort: parse responseData=... from toString()
        String s = String.valueOf(toolResponse);
        Matcher m = RESPONSE_DATA_IN_TOSTRING.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    /**
     * If responseData is a JSON string, parse it into Map/List so we can serialize cleanly.
     * Also unwrap "\"{...}\"" if your tool double-encoded JSON.
     */
    private static Object normalizeResponseData(Object responseData) {
        if (responseData == null) return null;

        if (responseData instanceof String s) {
            String t = s.trim();
            if (t.isEmpty()) return "";

            try {
                // If it's JSON, parse it
                char c0 = t.charAt(0);
                if (c0 == '{' || c0 == '[' || c0 == '"') {
                    JsonNode node = MAPPER.readTree(t);

                    // unwrap "\"{...}\""
                    if (node.isTextual()) {
                        return normalizeResponseData(node.asText());
                    }

                    return MAPPER.convertValue(node, Object.class);
                }
            } catch (Exception ignored) {
                // not JSON -> keep as plain string
            }

            return t;
        }

        // Map/List/record/etc
        return responseData;
    }

    /**
     * If it's already a JSON object/array string, return as-is.
     * Otherwise serialize to JSON.
     */
    private static String toJsonText(Object o) {
        if (o == null) return "";

        if (o instanceof String s) {
            String t = s.trim();
            if (t.startsWith("{") || t.startsWith("[")) return t;
            return t;
        }

        return toJson(o);
    }

    private static String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private static Object invoke(Object obj, String method) {
        try {
            return obj.getClass().getMethod(method).invoke(obj);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object field(Object obj, String fieldName) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safeText(Message message) {
        String t = message.getText();
        return t == null ? "" : t;
    }
}
