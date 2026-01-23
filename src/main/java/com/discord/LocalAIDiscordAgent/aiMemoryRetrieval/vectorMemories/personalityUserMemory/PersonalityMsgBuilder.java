package com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.vectorMemories.personalityUserMemory;

import java.util.List;

public class PersonalityMsgBuilder {

    protected static String buildUserPersonalityContextMsg(List<String> memories, String userId) {

        if (memories == null || memories.isEmpty()) return "";

        return """
                [LONG_TERM_MEMORY:USER_PERSONALITY]
                user_id: %s
                usage: reference_only
                scope: tone_and_preferences
                notes:
                - Use only if clearly relevant to the current message.
                - Influence HOW you respond (tone, framing, defaults), not WHAT new information you introduce.
                - Do not mention or quote these items unless the user explicitly asks.
                
                items:
                %s
                [/LONG_TERM_MEMORY:USER_PERSONALITY]
                """.formatted(userId, formatMemoryItems(memories));
    }

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
