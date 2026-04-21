package com.discord.LocalAIDiscordAgent.memory.ragMemory.webQAMemory;

import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.records.WebSearchRecords.MergedWebQAItem;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.records.WebSearchRecords.WebQAMemory;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.service.WebSearchMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class WebQAService {

    private final WebSearchMemoryService webSearchMemoryService;
    private final MapperUtils mapUtils;

    public WebQAService(WebSearchMemoryService webSearchMemoryService, MapperUtils mapperUtils) {
        this.webSearchMemoryService = webSearchMemoryService;
        this.mapUtils = mapperUtils;
    }

    public List<MergedWebQAItem> getWebQAResults(String query) {

        if (query == null || query.isEmpty()) {
            return null;
        }

        WebQAMemory webQAMemory = webSearchMemoryService.searchExistingContent(query);

        if (webQAMemory == null) {
            return null;
        }

        List<MergedWebQAItem> results = webQAMemory.results();

        if (results == null || results.isEmpty()) {
            return null;
        }

        return results;
    }

    public String getWebQAResultsAsRecordString(String query) {

        if (query == null || query.isEmpty()) {
            return null;
        }

        WebQAMemory webQAMemory = webSearchMemoryService.searchExistingContent(query);

        if (webQAMemory == null) {
            return null;
        }

        List<MergedWebQAItem> results = webQAMemory.results();

        if (results == null || results.isEmpty()) {
            return null;
        }

        TestWebQAResults testWebQAResults = new TestWebQAResults(results);

        return mapUtils.valuesToString(testWebQAResults);
    }

    public record TestWebQAResults(
            List<MergedWebQAItem> results
    ) {
    }

}
