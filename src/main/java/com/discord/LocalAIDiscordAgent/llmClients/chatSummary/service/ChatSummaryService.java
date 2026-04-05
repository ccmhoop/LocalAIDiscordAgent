package com.discord.LocalAIDiscordAgent.llmClients.chatSummary.service;


import com.discord.LocalAIDiscordAgent.llmClients.chatSummary.records.SummaryRecords.Fact;
import com.discord.LocalAIDiscordAgent.llmClients.chatSummary.records.SummaryRecords.MemoryState;
import com.discord.LocalAIDiscordAgent.llmClients.chatSummary.records.SummaryRecords.Turn;
import com.discord.LocalAIDiscordAgent.llmClients.chatSummary.model.ChatSummary;
import com.discord.LocalAIDiscordAgent.llmClients.chatSummary.repository.ChatSummaryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatSummaryService {

    private final ChatSummaryRepository repo;
    private final ChatSummaryUpdaterService updater;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public ChatSummaryService(ChatSummaryRepository chatSummaryRepository,
                              ChatSummaryUpdaterService chatSummaryUpdaterService
//                              ObjectMapper objectMapper
    ) {
        this.repo = chatSummaryRepository;
        this.updater = chatSummaryUpdaterService;
//        this.objectMapper = objectMapper;
    }

    @Transactional
    public MemoryState updateIfNeeded(String conversationId, List<Turn> turns) {

        ChatSummary entity = repo.findById(conversationId)
                .orElseGet(() -> new ChatSummary(conversationId));

        List<Fact> facts = readFacts(entity.getFactsJson());

        if (!turns.isEmpty()) {
            List<String> ids = new ArrayList<>(turns.size());
            turns.forEach(turn -> ids.add(turn.id()));

            List<Fact> clonedFacts = new ArrayList<>(ids.size());
            for (String id : ids) {
                facts.stream()
                        .filter(fact -> fact.evidence().stream().anyMatch(e -> e.turnId().equals(id)))
                        .forEach(clonedFacts::add);
            }

            facts = clonedFacts;
        }

        MemoryState current = new MemoryState(
                entity.getSummary(),
                facts,
                entity.getLastSummarizedTs()
        );

        MemoryState updated = updater.updateIfNeeded(current, turns);

        // Persist only if changed (optional optimization)
        String updatedFactsJson = writeFacts(updated.facts());
        entity.apply(updated, updatedFactsJson);
        repo.save(entity);

        return updated;
    }

    private List<Fact> readFacts(String json) {
        try {
            if (json == null || json.isBlank()) return List.of();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            // If corrupted, fail soft
            return List.of();
        }
    }

    private String writeFacts(List<Fact> facts) {
        try {
            return objectMapper.writeValueAsString(facts == null ? List.of() : facts);
        } catch (Exception e) {
            return "[]";
        }
    }

}