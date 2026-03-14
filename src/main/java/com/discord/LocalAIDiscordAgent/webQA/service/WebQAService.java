package com.discord.LocalAIDiscordAgent.webQA.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.WebQAMemory;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchMemoryService;
import org.springframework.stereotype.Service;

@Service
public class WebQAService {

    private final DiscGlobalData discGlobalData;
    private final WebSearchMemoryService webSearchMemoryService;

    public WebQAService(DiscGlobalData discGlobalData, WebSearchMemoryService webSearchMemoryService) {
        this.discGlobalData = discGlobalData;
        this.webSearchMemoryService = webSearchMemoryService;
    }

    public WebQAMemory getWebQAResults(){
        return webSearchMemoryService.searchExistingContent(discGlobalData.getUserMessage());
    }

}
