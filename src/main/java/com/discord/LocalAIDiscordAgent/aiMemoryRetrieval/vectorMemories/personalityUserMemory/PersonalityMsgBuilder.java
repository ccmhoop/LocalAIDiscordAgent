package com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.vectorMemories.personalityUserMemory;

import java.util.List;

public class PersonalityMsgBuilder {

    protected static String buildUserPersonalityContextMsg(List<String> memories, String userId) {

        if (memories.isEmpty()) return "";

        return """
                You have an established sense of personality familiarity with the user %s.
                These memories reflect habitual tone, preferences, and conversational tendencies.
                
                They are not facts to be stated, explained, or referenced directly.
                Allow them to influence how you respond, not what new information you introduce.
                
                --------------------- Personality Familiarity ---------------------
                %s
                ------------------------------------------------------------------
                """.formatted(userId, String.join("\n", memories));
    }
}
