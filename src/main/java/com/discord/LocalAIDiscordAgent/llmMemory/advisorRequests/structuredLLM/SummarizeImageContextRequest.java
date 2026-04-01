package com.discord.LocalAIDiscordAgent.llmMemory.advisorRequests.structuredLLM;

import com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.records.StructuredLLMContextRecord;
import com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.request.StructuredLLMRequest;
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolSummaryService.ContextSummary;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;

import java.util.List;

public class SummarizeImageContextRequest extends StructuredLLMRequest {

    private static final String SYSTEM_MESSAGE = """
            You are a strict visual-context summarizer for image generation.
            
            Primary objective:
            - describe the content visually for image context in a clear and concise manner.
            
            <image_context>
            %s
            </image_context>
            """;

    public SummarizeImageContextRequest(List<MergedWebQAItem> vectorDBResults) {
        super(ContextSummary.class, SYSTEM_MESSAGE, new StructuredLLMContextRecord(
                null,
                null,
                vectorDBResults,
                null
        ));
    }
}
