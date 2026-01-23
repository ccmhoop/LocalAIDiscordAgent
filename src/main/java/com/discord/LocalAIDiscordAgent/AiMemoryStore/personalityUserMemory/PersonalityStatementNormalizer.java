package com.discord.LocalAIDiscordAgent.AiMemoryStore.personalityUserMemory;

import org.springframework.stereotype.Component;

@Component
public class PersonalityStatementNormalizer {

    public String normalize(String rawStatement) {

        if (rawStatement == null || rawStatement.isBlank()) {
            return null;
        }

        String normalized = rawStatement.trim();

        // Remove first-person phrasing
        normalized = normalized.replaceAll("(?i)\\b(i|me|my|mine)\\b", "User");

        // Remove emotional intensifiers
        normalized = normalized.replaceAll(
                "(?i)\\b(hate|love|annoyed|frustrated|angry|really|very)\\b",
                ""
        );

        // Normalize negations
        normalized = normalized.replaceAll("(?i)don't want to", "avoids");
        normalized = normalized.replaceAll("(?i)do not want to", "avoids");
        normalized = normalized.replaceAll("(?i)never", "avoids");
        normalized = normalized.replaceAll("(?i)always", "consistently");

        // Ensure third-person prefix
        if (!normalized.toLowerCase().startsWith("user")) {
            normalized = "User " + normalized;
        }

        // Clean spacing
        normalized = normalized.replaceAll("\\s{2,}", " ").trim();

        return normalized.endsWith(".") ? normalized : normalized + ".";
    }


}
