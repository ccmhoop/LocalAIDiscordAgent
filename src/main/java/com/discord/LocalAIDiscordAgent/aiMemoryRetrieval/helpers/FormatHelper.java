package com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.helpers;

import java.util.List;

public final class FormatHelper {

    public static String formatMemoryItems(List<String> memories) {
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
