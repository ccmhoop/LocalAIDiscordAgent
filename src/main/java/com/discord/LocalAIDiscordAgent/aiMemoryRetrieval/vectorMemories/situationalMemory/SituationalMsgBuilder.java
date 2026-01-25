package com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.vectorMemories.situationalMemory;

import com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.helpers.FormatHelper;

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
                - Apply only when message clearly refers to this situation
                - Don't introduce/revive situations from memory
                - Don't mention/quote unless explicitly asked

                items:
                %s
                [/LONG_TERM_MEMORY:SITUATIONAL]
                """.formatted(userId, FormatHelper.formatMemoryItems(memories));
    }

}
