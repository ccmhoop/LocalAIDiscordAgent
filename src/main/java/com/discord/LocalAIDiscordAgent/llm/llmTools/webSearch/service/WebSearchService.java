package com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.llmCall.WebSearchToolCall;
import com.discord.LocalAIDiscordAgent.llm.llmTools.service.ToolSummaryService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.preparation.WebSearchPreparation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WebSearchService {

    private final WebSearchToolCall webSearchToolCall;
    private final ToolSummaryService toolSummaryService;
    private final WebSearchPreparation webSearchPreparation;

    public WebSearchService(
            ToolSummaryService toolSummaryService,
            WebSearchToolCall webSearchToolCall, WebSearchPreparation webSearchPreparation
    ) {
        this.webSearchToolCall = webSearchToolCall;
        this.toolSummaryService = toolSummaryService;
        this.webSearchPreparation = webSearchPreparation;
    }

    public void handleWebSearch(DiscGlobalData discGlobalData, PromptData promptData) {
        webSearchPreparation.prepare(discGlobalData, promptData);
        if (promptData.isWebSearchRequired()) {
            String toolContext = webSearchToolCall.executeTools(discGlobalData);
            promptData.setRetrievedContext(toolContext);
        }

        if (promptData.getRetrievedContext() != null){
            callSummaryTool(discGlobalData, promptData);
        }
    }

    private void callSummaryTool(DiscGlobalData discGlobalData, PromptData promptData) {
        String retrievedContextString = promptData.getRetrievedContext();
        if (retrievedContextString != null && !retrievedContextString.isEmpty()) {
            String summary = toolSummaryService.summerizeToolResults(retrievedContextString, discGlobalData );
            promptData.setSummary(summary);
        }else {
            promptData.setSummary(null);
        }

    }


}
