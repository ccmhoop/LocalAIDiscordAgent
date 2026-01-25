package com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.vectorMemories.personalityUserMemory;

import com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.helpers.FormatHelper;

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
                - Use only if relevant to current message
                - Affects HOW you respond (tone/framing), not WHAT information you introduce
                - Don't mention/quote unless explicitly asked

                items:
                %s
                [/LONG_TERM_MEMORY:USER_PERSONALITY]
                """.formatted(userId, FormatHelper.formatMemoryItems(memories));
    }

}
