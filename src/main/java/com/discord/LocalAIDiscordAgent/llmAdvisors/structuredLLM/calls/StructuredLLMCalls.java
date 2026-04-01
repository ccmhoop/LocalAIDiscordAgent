package com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.calls;

import com.discord.LocalAIDiscordAgent.comfyui.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.llm.StructuredLLM;
import com.discord.LocalAIDiscordAgent.comfyui.advisorRequests.structuredLLM.GenerateImageSettingsRequest;
import com.discord.LocalAIDiscordAgent.llmMemory.advisorRequests.structuredLLM.SummarizeImageContextRequest;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.discord.LocalAIDiscordAgent.queryGenerator.advisorRequests.structuredLLM.GenerateImageQueryRequest;
import com.discord.LocalAIDiscordAgent.queryGenerator.advisorRequests.structuredLLM.GenerateVectorQueryRequest;
import com.discord.LocalAIDiscordAgent.queryGenerator.records.QueryRecord;
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolSummaryService.ContextSummary;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StructuredLLMCalls {

    private final StructuredLLM llm;
    private final DiscGlobalData discGlobalData;
    private final PromptData promptData;

    public StructuredLLMCalls(
            StructuredLLM structuredLLM,
            DiscGlobalData discGlobalData, PromptData promptData
    ) {
        this.discGlobalData = discGlobalData;
        this.llm = structuredLLM;
        this.promptData = promptData;
    }

    public String generateVectorQuery() {
        Record record = llm.call(new GenerateVectorQueryRequest(discGlobalData));
        if (record instanceof QueryRecord(String query)) {
            return query;
        }
        return null;
    }

    public String generateImageQuery() {
        Record record = llm.call(new GenerateImageQueryRequest());
        if (record instanceof QueryRecord(String query)) {
            return query;
        }
        return null;
    }

    public String summarizeImageContext(){
        List<MergedWebQAItem> vectorDBResults = promptData.getVectorDBMemory();
        Record record = llm.call(new SummarizeImageContextRequest(vectorDBResults));
        if (record instanceof ContextSummary(String summary)) {
            promptData.setSummary(summary);
            return summary;
        }
        return null;
    }

    public ImageSettingsRecord generateImageSettings() {
        String summary = promptData.getSummary();
        Record record = llm.call(new GenerateImageSettingsRequest(summary));
        if (record instanceof ImageSettingsRecord recordOutPut) {
            return recordOutPut;
        }
        return null;
    }

}
