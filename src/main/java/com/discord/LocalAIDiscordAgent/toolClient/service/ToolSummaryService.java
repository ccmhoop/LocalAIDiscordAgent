package com.discord.LocalAIDiscordAgent.toolClient.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
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

    private final DiscGlobalData discGlobalData;
    private final ChatClient structuredToolClient;

    private RecentMessage recentAssistantMsg;
    private String toolResults;

    public ToolSummaryService(
            DiscGlobalData discGlobalData,
            ChatClient structuredToolClient
    ) {
        this.structuredToolClient = structuredToolClient;
        this.discGlobalData = discGlobalData;
    }

    public String summerizeToolResults(
            String results,
            RecentMessage recentAssistantMsg
    ) {
        this.recentAssistantMsg = recentAssistantMsg;
        this.toolResults = results;

        String summarizedToolResults = summarizeToolResults();

        if (summarizedToolResults.isEmpty()){
            return "";
        }

        return summarizedToolResults;
    }

    private String summarizeToolResults() {
        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(WebSearchToolResult.class)
                .maxRepeatAttempts(3)
                .build();

        var summaryConv = new BeanOutputConverter<>(WebSearchToolResult.class);

        Map<String, Object> summarySchema = summaryConv.getJsonSchemaMap();

        WebSearchToolResult modelOut = structuredToolClient.prompt()
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
                .entity(WebSearchToolResult.class);

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

        if (recentAssistantMsg != null) {
            sb.append("assistant Message: ").append(recentAssistantMsg.content()).append("\n");
        }

        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public record WebSearchToolResult(String summary) {
    }

}
