package com.discord.LocalAIDiscordAgent.AiMemoryStore.personalityUserMemory;

import org.springframework.stereotype.Component;

@Component
public class PersonalityStatementNormalizer {

    public String normalize(String rawStatement) {

        if (rawStatement == null || rawStatement.isBlank()) {
            return null;
        }

        String normalized = rawStatement.trim();

        normalized = normalized.replaceAll("(?i)\\b(i|me|my|mine)\\b", "User");

        normalized = normalized.replaceAll(
                "(?i)\\b(hate|love|annoyed|frustrated|angry|really|very)\\b",
                ""
        );

        normalized = normalized.replaceAll("(?i)don't want to", "avoids");
        normalized = normalized.replaceAll("(?i)do not want to", "avoids");
        normalized = normalized.replaceAll("(?i)never", "avoids");
        normalized = normalized.replaceAll("(?i)always", "consistently");

        if (!normalized.toLowerCase().startsWith("user")) {
            normalized = "User " + normalized;
        }

        normalized = normalized.replaceAll("\\s{2,}", " ").trim();

        return normalized.endsWith(".") ? normalized : normalized + ".";
    }


}
