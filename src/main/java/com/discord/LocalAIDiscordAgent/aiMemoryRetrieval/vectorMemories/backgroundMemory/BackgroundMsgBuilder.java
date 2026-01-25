package com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.vectorMemories.backgroundMemory;

import com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.helpers.FormatHelper;

import java.util.List;

public class BackgroundMsgBuilder {

    protected static String buildBackgroundContextMsg(String userId, List<String> memories) {

        if (memories == null || memories.isEmpty()) return "";

        return """
                [LONG_TERM_MEMORY:USER]
                user_id: %s
                usage: reference_only
                notes:
                - Apply only if relevant to current message
                - Don't mention/quote unless explicitly asked

                items:
                %s
                [/LONG_TERM_MEMORY:USER]
                """.formatted(
                userId,
                FormatHelper.formatMemoryItems(memories)
        );
    }

    protected static String buildSubjectBackgroundContextMsg(List<String> memories) {

        if (memories == null || memories.isEmpty()) return "";

        return """
                [LONG_TERM_MEMORY:SUBJECT]
                usage: reference_only
                notes:
                - Only for subjects explicitly mentioned
                - Ignore if not referenced in current message
                - Don't mention/quote unless explicitly asked

                items:
                %s
                [/LONG_TERM_MEMORY:SUBJECT]
                """.formatted(FormatHelper.formatMemoryItems(memories));
    }

}
