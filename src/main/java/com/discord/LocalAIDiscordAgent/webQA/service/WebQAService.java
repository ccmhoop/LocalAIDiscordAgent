package com.discord.LocalAIDiscordAgent.webQA.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.WebQAMemory;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchMemoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebQAService {

    private final DiscGlobalData discGlobalData;
    private final WebSearchMemoryService webSearchMemoryService;

    public WebQAService(DiscGlobalData discGlobalData, WebSearchMemoryService webSearchMemoryService) {
        this.discGlobalData = discGlobalData;
        this.webSearchMemoryService = webSearchMemoryService;
    }

    public List<MergedWebQAItem> getWebQAResults(){
        return webSearchMemoryService.searchExistingContent(discGlobalData.getUserMessage()).results();
    }

}
