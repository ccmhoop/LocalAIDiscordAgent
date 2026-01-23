package com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.vectorMemories.backgroundMemory;

import java.util.List;

public class BackgroundMsgBuilder {

    protected static String buildBackgroundContextMsg(String userId, List<String> memories) {

        if (memories == null || memories.isEmpty()) return "";

        // Keep this as "reference data", not a second instruction block.
        // Qwen3 follows system rules better when retrieval text is clean and uniform.
        return """
                [LONG_TERM_MEMORY:USER]
                user_id: %s
                usage: reference_only
                notes:
                - Background familiarity. Apply only if clearly relevant to the current message.
                - Do not mention or quote unless the user explicitly asks.
                
                items:
                %s
                [/LONG_TERM_MEMORY:USER]
                """.formatted(
                userId,
                formatMemoryItems(memories)
        );
    }

    protected static String buildSubjectBackgroundContextMsg(List<String> memories) {

        if (memories == null || memories.isEmpty()) return "";

        return """
                [LONG_TERM_MEMORY:SUBJECT]
                usage: reference_only
                notes:
                - Background familiarity about a subject explicitly mentioned by the user.
                - Ignore if the subject is not explicitly referenced in the current message.
                - Do not mention or quote unless the user explicitly asks.
                
                items:
                %s
                [/LONG_TERM_MEMORY:SUBJECT]
                """.formatted(formatMemoryItems(memories));
    }

    /**
     * Formats memories as stable bullet items and strips accidental leading/trailing whitespace.
     * This reduces prompt noise and makes retrieval content easier for the model to ground on.
     */
    private static String formatMemoryItems(List<String> memories) {
        var sb = new StringBuilder();
        for (int i = 0; i < memories.size(); i++) {
            var m = memories.get(i);
            if (m == null) continue;
            m = m.trim();
            if (m.isEmpty()) continue;
            sb.append("- ").append(m);
            if (i < memories.size() - 1) sb.append("\n");
        }
        return sb.toString().isBlank() ? "- (none)" : sb.toString();
    }


}
