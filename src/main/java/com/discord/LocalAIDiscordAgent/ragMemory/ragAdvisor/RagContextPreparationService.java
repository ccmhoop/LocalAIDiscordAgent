package com.discord.LocalAIDiscordAgent.ragMemory.ragAdvisor;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import com.discord.LocalAIDiscordAgent.ragMemory.webQAMemory.WebQAService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RagContextPreparationService {

    private final RagContextSelectionService selectionService;
    private final WebQAService webQAService;
    private final MapperUtils mapperUtils;

    public RagContextPreparationService(
            RagContextSelectionService selectionService,
            WebQAService webQAService,
            MapperUtils mapperUtils
    ) {
        this.selectionService = selectionService;
        this.webQAService = webQAService;
        this.mapperUtils = mapperUtils;
    }

    public void prepare(DiscGlobalData discGlobalData, PromptData promptData) {
//        PromptData promptData = new PromptData(mapperUtils);
        String userMessage = discGlobalData.getUserMessage();
//        String normalizedUserMessage = normalize(userMessage);
//        if (normalizedUserMessage == null) {
//            return false;
//        }

        String query = selectionService.buildQuery(userMessage);
        log.info("Retrieved-context query: {}", query);

        if (query == null) {
//            return false;
        return;
        }

        List<MergedWebQAItem> documents = Optional.ofNullable(webQAService.getWebQAResults(query))
                .orElse(List.of());

        if (documents.isEmpty()) {
            return;
        }

        String retrievedContext = mapperUtils.valuesToString(new RetrievedContextRecord(documents));
        retrievedContext = normalize(retrievedContext);

        boolean softRelevant = selectionService.isRelevant(retrievedContext, userMessage);
        log.info("Retrieved-context soft relevance: {}", softRelevant);

        if (!softRelevant) {
            return;
        }

        boolean hardRelevant = isUsableRetrievedContext(documents, retrievedContext);
        log.info("Retrieved-context hard gate passed: {}", hardRelevant);

        if (!hardRelevant) {
            return;
        }

        promptData.setQueryString(query);
        promptData.setVectorDBResults(documents);
        promptData.setRetrievedContext(retrievedContext);

    }

    private boolean isUsableRetrievedContext(List<MergedWebQAItem> documents, String retrievedContext) {
        return documents != null
                && !documents.isEmpty()
                && retrievedContext != null
                && !retrievedContext.isBlank();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public record RetrievedContextRecord(List<MergedWebQAItem> documents) {
    }
}