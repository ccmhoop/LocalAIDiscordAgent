package com.discord.LocalAIDiscordAgent.llm.llmTools.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ToolSummaryService {

    private final ChatClient structuredToolClient;
    private final PromptData promptData;
    private  DiscGlobalData discGlobalData;

    private String toolResults;

    public ToolSummaryService(
            ChatClient llmStructuredClient, PromptData promptData
    ) {
        this.structuredToolClient = llmStructuredClient;
        this.promptData = promptData;
    }

    public String summerizeToolResults(
            String retrievedContext,
            DiscGlobalData discGlobalData

    ) {
        this.discGlobalData = discGlobalData;
        this.toolResults = retrievedContext;

        String summarizedToolResults = summarizeToolResults();

        if (summarizedToolResults.isEmpty()){
            return "";
        }

        promptData.setRetrievedContext(summarizedToolResults);
        return summarizedToolResults;
    }

    private String summarizeToolResults() {
        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(ContextSummary.class)
                .maxRepeatAttempts(3)
                .build();

        var summaryConv = new BeanOutputConverter<>(ContextSummary.class);

        Map<String, Object> summarySchema = summaryConv.getJsonSchemaMap();

        ContextSummary modelOut = structuredToolClient.prompt()
                .options(OllamaChatOptions.builder()
                        .format(summarySchema)
                        .build()
                )
                .system("Create a summary using the tool results! Try to summarize based on the user's message and assistant message if available.")
                .user(user ->
                                user.text(userSummaryMSG())
                )
                .advisors(validation)
                .call()
                .entity(ContextSummary.class);

        log.info("Tool summary: {}", modelOut);

        if (modelOut == null) {
            return "";
        }

        return safe(modelOut.summary);
    }

    private String userSummaryMSG() {

        StringBuilder sb = new StringBuilder();

        sb.append("tool results: ").append(this.toolResults).append("\n");

        if (discGlobalData.getUserMessage() != null) {
            sb.append("user Message: ").append(discGlobalData.getUserMessage()).append("\n");
        }

        if (discGlobalData.getLastAssistantMsg() != null) {
            sb.append("assistant Message: ").append(discGlobalData.getLastAssistantMsg().content()).append("\n");
        }

        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public record ContextSummary(String summary) {
    }

}
