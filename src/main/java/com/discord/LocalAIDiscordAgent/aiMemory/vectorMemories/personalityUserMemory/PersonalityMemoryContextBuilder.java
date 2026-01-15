package com.discord.LocalAIDiscordAgent.aiMemory.vectorMemories.personalityUserMemory;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PersonalityMemoryContextBuilder {

    private final VectorStore vectorStore;
    private final PersonalityExtractor extractor;
    private final PersonalityStatementNormalizer normalizer;
    private final PersonalitySaver personalitySaver;

    public PersonalityMemoryContextBuilder(VectorStore vectorStoreScottishConfig, PersonalityExtractor extractor, PersonalityStatementNormalizer normalizer, PersonalitySaver personalitySaver) {
        this.vectorStore = vectorStoreScottishConfig;
        this.extractor = extractor;
        this.normalizer = normalizer;
        this.personalitySaver = personalitySaver;
    }

    public void buildUserPersonalityMemories(String userId, List<Message> message) {

        Set<String> results = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query("")
                                .topK(8)
                                .filterExpression("tier == 'PERSONALITY' && userId == '" + userId + "'")
                                .build()
                ).stream()
                .map(Document::getText)
                .collect(Collectors.toSet());

        if (results.isEmpty()) return;

        message.add(
                new SystemMessage(
                        PersonalityMsgBuilder.buildUserPersonalityContextMsg(
                                results.stream().toList(),
                                userId
                        )
                )
        );

    }

    public void processPersonality(String userId, String messageText) {

        extractor.extract(messageText).ifPresent(candidate -> {

            String normalized = normalizer.normalize(candidate.rawText());

            personalitySaver.write(
                    userId,
                    candidate.subject(),
                    normalized,
                    candidate.aliases()
            );
        });
    }


}
