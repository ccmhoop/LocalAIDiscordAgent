package com.discord.LocalAIDiscordAgent.aiMemory.service;

import com.discord.LocalAIDiscordAgent.aiMemory.enums.MemoryTier;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class MemoryWriteService {

    private final VectorStore vectorStore;

    public MemoryWriteService(VectorStore vectorStoreScottishConfig) {
        this.vectorStore = vectorStoreScottishConfig;
    }

    public void storePersonalityMemory(String userId, String content) {

        Document doc = new Document(
                content,
                Map.of(
                        "tier", "PERSONALITY",
                        "userId", userId,
                        "createdAt", Instant.now().toString()
                )
        );

        vectorStore.add(List.of(doc));
    }

    public void storeSituationalMemory(String userId, String content) {

        Document doc = new Document(
                content,
                Map.of(
                        "tier", "SITUATIONAL",
                        "userId", userId,
                        "createdAt", Instant.now().toString()
                )
        );

        vectorStore.add(List.of(doc));
    }

    public void storeBackgroundMemory(String userId, String content) {

        Document doc = new Document(
                content,
                Map.of(
                        "tier", MemoryTier.BACKGROUND.name(),
                        "userId", userId,
                        "createdAt", Instant.now().toString()
                )
        );

        vectorStore.add(List.of(doc));
    }


}
