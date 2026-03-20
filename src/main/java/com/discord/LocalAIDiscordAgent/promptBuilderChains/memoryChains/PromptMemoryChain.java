package com.discord.LocalAIDiscordAgent.promptBuilderChains.memoryChains;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolQueryGenerationService;
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolRelevantMemoryService;
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolService;
import com.discord.LocalAIDiscordAgent.vectorMemory.webQAMemory.WebQAService;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptMemoryChain {

    private final ToolService toolService;
    private final WebQAService webQAService;
    private final DiscGlobalData discGlobalData;
    private final ToolRelevantMemoryService toolRelevantMemoryService;
    private final ToolQueryGenerationService toolQueryGenerationService;

    public PromptMemoryChain(
            ToolService toolService,
            WebQAService webQAService,
            DiscGlobalData discGlobalData,
            ToolRelevantMemoryService toolRelevantMemoryService,
            ToolQueryGenerationService toolQueryGenerationService) {
        this.toolQueryGenerationService = toolQueryGenerationService;
        this.toolRelevantMemoryService = toolRelevantMemoryService;
        this.discGlobalData = discGlobalData;
        this.webQAService = webQAService;
        this.toolService = toolService;
    }


    public void executeMemoryChain() {
        boolean isMemoryRelevant = toolRelevantMemoryService.isMemoryRelevant();

        String query = toolQueryGenerationService.generateQuery(isMemoryRelevant);
        List<MergedWebQAItem> webQAResults = webQAService.getWebQAResults(query);
        String toolSummary = toolService.executeTools(getLastAssistantMsg(), webQAResults);
    }

    private RecentMessage getLastAssistantMsg() {
        if (discGlobalData.getRecentMessages() == null || discGlobalData.getRecentMessages().isEmpty()) return null;
        return new RecentMessage(
                discGlobalData.getRecentMessages().getLast().timestamp(),
                MessageType.ASSISTANT.toString(),
                discGlobalData.getRecentMessages().getLast().content()
        );
    }

}
