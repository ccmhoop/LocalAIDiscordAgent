package com.discord.LocalAIDiscordAgent.aiMemory.vectorMemories.situationalMemory;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SituationalMemoryContextBuilder {

    private final VectorStore vectorStore;

    public SituationalMemoryContextBuilder(VectorStore vectorStoreScottishConfig) {
        this.vectorStore = vectorStoreScottishConfig;
    }

    public void buildSituationalMemories(String userId, String userMessage, List<Message> message) {

        List<Document> retrievedDocuments = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userMessage)
                        .topK(5)
                        .similarityThreshold(0.78f)
                        .filterExpression(
                                "tier == 'SITUATIONAL' && userId == '" + userId + "'"
                        ).build()
        );

        Set<String> results = retrievedDocuments.stream()
                .map(Document::getText)
                .filter(text -> isUserTriggered(text, userMessage))
                .collect(Collectors.toSet());

        message.add(
                new SystemMessage(
                        SituationalMsgBuilder.buildSituationalContextMsg(
                                results.stream().toList(),
                                userId
                        )
                )
        );
    }

    private boolean isUserTriggered(String memory, String message) {
        String msg = message.toLowerCase();
        return Arrays.stream(msg.split("\\W+"))
                .anyMatch(token -> token.length() > 3 && memory.toLowerCase().contains(token));
    }

}
