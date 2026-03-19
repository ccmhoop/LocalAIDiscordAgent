package com.discord.LocalAIDiscordAgent.vectorMemory.longTermMemory;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LongTermMemoryService {

    private static final int SEARCH_TOP_K = 2;
    private static final String DATE_KEY = "timestamp";
    private static final double RETRIEVAL_SIMILARITY_THRESHOLD = 0.60;
    private static final String ASSISTANT_MESSAGE_KEY = "assistantMessage";
    private static final String TIER_LONG_TERM_CHAT_MEMORY = "long_term_chat_memory";

    private final DiscGlobalData discGlobalData;
    private final VectorStore vectorStore;
    private final DocumentWriter writer;

    private boolean shouldSave = true;

    public LongTermMemoryService(DiscGlobalData discGlobalData, VectorStore longTermVectorMemory) {
        this.writer = longTermVectorMemory;
        this.discGlobalData = discGlobalData;
        this.vectorStore = longTermVectorMemory;
    }

    public List<LongTermMemoryData> getLongTermMemory() {
        try {
            List<Document> matches = getDocuments();

            if (matches.isEmpty()) {
                shouldSave = true;
                return null;
            } else {
                shouldSave = false;
            }

            return getList(matches);
        } catch (Exception e) {
            log.error("Error searching existing content: {}", e.getMessage(), e);
            return null;
        }
    }

    private List<Document> getDocuments() {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .similarityThreshold(RETRIEVAL_SIMILARITY_THRESHOLD)
                        .filterExpression("tier == '" + TIER_LONG_TERM_CHAT_MEMORY + "'")
                        .filterExpression("conversationID == '" + discGlobalData.getConversationId() + "'")
                        .filterExpression("userID == '" + discGlobalData.getUserId() + "'")
                        .query(discGlobalData.getUserMessage())
                        .topK(SEARCH_TOP_K)
                        .build()
        );
    }

    private static List<LongTermMemoryData> getList(List<Document> matches) {
        return matches.stream()
                .map(document -> new LongTermMemoryData(
                                String.valueOf(document.getMetadata().get(DATE_KEY)),
                                document.getText(),
                                String.valueOf(document.getMetadata().get(ASSISTANT_MESSAGE_KEY))
                        )
                )
                .toList();
    }

    public void saveLongTermMemory(AssistantMessage assistantMessage) {
        if (!shouldSave) {
            shouldSave = true;
            return;
        }
        DocumentReader reader = () -> List.of(buildSourceDocument(assistantMessage));
        List<Document> docs = reader.read();
        if (!docs.isEmpty()) writer.write(docs);
    }

    private Document buildSourceDocument(AssistantMessage assistantMessage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tier", TIER_LONG_TERM_CHAT_MEMORY);
        metadata.put("userID", discGlobalData.getUserId());
        metadata.put(ASSISTANT_MESSAGE_KEY, assistantMessage.getText());
        metadata.put("conversationID", discGlobalData.getConversationId());
        metadata.put(DATE_KEY, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());

        return new Document(discGlobalData.getUserMessage(), metadata);
    }

    public record LongTermMemoryData(
            String date,
            String userMessage,
            String assistantMessage) {
    }
}
