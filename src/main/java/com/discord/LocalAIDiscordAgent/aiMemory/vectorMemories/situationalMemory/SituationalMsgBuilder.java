package com.discord.LocalAIDiscordAgent.aiMemory.vectorMemories.situationalMemory;

import java.util.List;

public class SituationalMsgBuilder {

    protected static String buildSituationalContextMsg(List<String> memories, String userId) {

        if (memories.isEmpty()) return "";

        return """
        The following represents situational familiarity related to the user %s.
        These memories apply only when the current message clearly refers to them.

        Do not introduce topics, facts, or assumptions based on this information
        unless the situation is explicitly active in the conversation.

        --------------------- Situational Familiarity ---------------------
        %s
        ------------------------------------------------------------------
        """.formatted(userId, String.join("\n", memories));
    }

}
