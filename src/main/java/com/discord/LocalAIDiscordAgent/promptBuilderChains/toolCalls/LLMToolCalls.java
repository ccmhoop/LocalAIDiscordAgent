package com.discord.LocalAIDiscordAgent.promptBuilderChains.toolCalls;

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

    public void callWebSearchTool() {
        String toolContext = toolService.executeTools();
        promptData.setRetrievedContext(toolContext);
    }

    public String callSummaryTool() {
        String retrievedContextString = promptData.getRetrievedContext();
        return toolSummaryService.summerizeToolResults(retrievedContextString);
    }


}
