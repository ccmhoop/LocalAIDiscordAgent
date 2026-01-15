package com.discord.LocalAIDiscordAgent.aiMemory.vectorMemories.backgroundMemory;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BackgroundMemoryContextBuilder {

    private final VectorStore vectorStore;

    public BackgroundMemoryContextBuilder(VectorStore vectorStoreScottishConfig) {
        this.vectorStore = vectorStoreScottishConfig;
    }

    public void buildBackgroundMemories(String userId, String userMessage, List<Message> message) {
        loadUserBackgroundMemories(userId, message);
        loadSubjectsBackgroundMemories(userMessage, message);
    }

    private void loadUserBackgroundMemories(String userId, List<Message> message) {

        Set<String> perSubject = vectorStore.similaritySearch(
                        SearchRequest.builder().query("")
                                .topK(5)
                                .filterExpression(
                                        "tier == 'BACKGROUND' && userId == '" + userId + "'"
                                ).build()
                ).stream()
                .map(Document::getText)
                .collect(Collectors.toSet());

        if (perSubject.isEmpty()) return;

        message.add(
                new SystemMessage(
                        BackgroundMsgBuilder.buildBackgroundContextMsg(
                                userId,
                                perSubject.stream().toList())
                )
        );

    }

    private void loadSubjectsBackgroundMemories(String userMessage, List<Message> message) {

        Set<String> subjects = resolveSubjects(userMessage);
        if (subjects.isEmpty()) {
            return;
        }

        List<String> results = new ArrayList<>();

        for (String subject : subjects) {
            List<String> perSubject = vectorStore.similaritySearch(
                            SearchRequest.builder()
                                    .query(userMessage)
                                    .topK(3)
                                    .filterExpression(
                                            "tier == 'BACKGROUND' && subject == '" + subject + "'"
                                    )
                                    .build()
                    ).stream()
                    .map(Document::getText)
                    .toList();

            results.addAll(perSubject);
        }

        if (results.isEmpty()) return;

        message.add(new SystemMessage(
                        BackgroundMsgBuilder.buildSubjectBackgroundContextMsg(results)
                )
        );

    }

    private Set<String> resolveSubjects(String userMessage) {

        String msg = userMessage.toLowerCase();

        List<Document> candidates = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userMessage)
                        .topK(10)
                        .filterExpression("tier == 'BACKGROUND'")
                        .build()
        );

        Map<String, Integer> scores = new HashMap<>();

        for (Document doc : candidates) {

            String subject = (String) doc.getMetadata().get("subject");
            Object aliasesObj = doc.getMetadata().get("aliases");

            if (subject == null) continue;

            int score = 0;

            // Strong signal: alias match
            if (aliasesObj instanceof List<?> aliases) {
                for (Object a : aliases) {
                    String alias = a.toString().toLowerCase();
                    if (!alias.isBlank() && msg.contains(alias)) {
                        score += 5;
                    }
                }
            }

            // Medium signal: subject token match
            if (msg.contains(subject)) {
                score += 3;
            }

            if (score > 0) {
                scores.merge(subject, score, Integer::sum);
            }
        }

        // Return all subjects with meaningful signal
        return scores.entrySet().stream()
                .filter(e -> e.getValue() >= 3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

}
