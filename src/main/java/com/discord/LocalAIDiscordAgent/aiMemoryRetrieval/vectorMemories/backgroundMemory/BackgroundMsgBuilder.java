package com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.vectorMemories.backgroundMemory;

import java.util.List;

public class BackgroundMsgBuilder {

    protected static String buildBackgroundContextMsg(String userId, List<String> memories) {

        if (memories.isEmpty()) return "";

        return """
                The following reflects long-term background familiarity about the user %s.
                This information provides contextual grounding and baseline assumptions.
                
                Do not present it as new information or surface it unprompted.
                Use it only to inform interpretation and response framing.
                
                --------------------- User Background ---------------------
                %s
                ----------------------------------------------------------
                """.formatted(userId, String.join("\n", memories));
    }

    protected static String buildSubjectBackgroundContextMsg(List<String> memories) {

        if (memories.isEmpty()) return "";

        return """
                The following reflects long-term background familiarity about a subject
                mentioned in the current user message.
                
                Use this information only for contextual understanding and assumptions.
                Never introduce it unless the subject is explicitly referenced.
                
                --------------------- Subject Background ---------------------
                %s
                --------------------------------------------------------------
                """.formatted(String.join("\n", memories));
    }

}
