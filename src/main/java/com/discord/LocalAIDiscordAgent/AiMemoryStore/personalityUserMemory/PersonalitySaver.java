package com.discord.LocalAIDiscordAgent.AiMemoryStore.personalityUserMemory;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PersonalitySaver {

    private static final String TIER_PERSONALITY = "PERSONALITY";

    private final VectorStore vectorStore;

    public PersonalitySaver(VectorStore vectorStoreScottishConfig) {
        this.vectorStore = vectorStoreScottishConfig;
    }

    public void write(
            String userId,
            String subject,
            String personalityStatement,
            List<String> aliases
    ) {

        if (personalityStatement == null || personalityStatement.isBlank()) {
            return;
        }

        if (isDuplicate(userId, personalityStatement)) {
            return;
        }

        Document document = new Document(
                personalityStatement,
                Map.of(
                        "tier", TIER_PERSONALITY,
                        "userId", userId,
                        "subject", subject,
                        "aliases", aliases == null ? List.of() : aliases
                )
        );

        vectorStore.add(List.of(document));
    }

    private boolean isDuplicate(String userId, String text) {

        List<Document> matches = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(text)
                        .topK(1)
                        .filterExpression(
                                "tier == '" + TIER_PERSONALITY + "' && userId == '" + userId + "'"
                        )
                        .build()
        );

        return !matches.isEmpty();
    }

}
