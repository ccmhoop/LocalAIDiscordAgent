package com.discord.LocalAIDiscordAgent.promptBuilderChains.toolCalls;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolService;
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolSummaryService;
import org.springframework.stereotype.Component;

@Component
public class LLMToolCalls {

    private final PromptData promptData;
    private final ToolService toolService;
    private final ToolSummaryService toolSummaryService;

    public LLMToolCalls(
            ToolSummaryService toolSummaryService,
            ToolService toolService,
            PromptData promptData
    ) {
        this.promptData = promptData;
        this.toolService = toolService;
        this.toolSummaryService = toolSummaryService;
    }

    public void callWebSearchTool(DiscGlobalData discGlobalData) {
        String toolContext = toolService.executeTools(discGlobalData);
        promptData.setRetrievedContext(toolContext);
    }

    public void callSummaryTool(PromptData data, DiscGlobalData discGlobalData) {
        String retrievedContextString = data.getRetrievedContext();
        if (retrievedContextString != null && !retrievedContextString.isEmpty()) {
            String summary = toolSummaryService.summerizeToolResults(retrievedContextString, discGlobalData );
            data.setSummary(summary);
        }else {
            data.setSummary(null);
        }

    }


}
