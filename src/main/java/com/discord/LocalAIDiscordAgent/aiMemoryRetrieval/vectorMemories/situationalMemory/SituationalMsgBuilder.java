package com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.vectorMemories.situationalMemory;

import java.util.List;

public class SituationalMsgBuilder {

    protected static String buildSituationalContextMsg(List<String> memories, String userId) {

        if (memories == null || memories.isEmpty()) return "";

        return """
                [LONG_TERM_MEMORY:SITUATIONAL]
                user_id: %s
                usage: reference_only
                scope: situational
                activation_rule: only_if_explicitly_active_in_current_message
                notes:
                - Apply only when the current message clearly refers to this situation.
                - Do not introduce or revive a situation from memory.
                - Do not mention or quote these items unless the user explicitly asks.
                
                items:
                %s
                [/LONG_TERM_MEMORY:SITUATIONAL]
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
