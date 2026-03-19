package com.discord.LocalAIDiscordAgent.vectorMemory.webQAMemory;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolQueryGenerationService;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.WebQAMemory;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class WebQAService {

    private final ToolQueryGenerationService toolQueryGenerationService;
    private final WebSearchMemoryService webSearchMemoryService;

    public WebQAService(
            WebSearchMemoryService webSearchMemoryService,
            ToolQueryGenerationService toolQueryGenerationService
    ) {
        this.toolQueryGenerationService = toolQueryGenerationService;
        this.webSearchMemoryService = webSearchMemoryService;
    }

    public List<MergedWebQAItem> getWebQAResults(List<RecentMessage> recentMessages) {
        String query = toolQueryGenerationService.generateQuery(recentMessages);

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

}
