package com.discord.LocalAIDiscordAgent.webQA.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.WebQAMemory;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchMemoryService;
import com.discord.LocalAIDiscordAgent.webSearch.tools.WebSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WebQAService {

    private final DiscGlobalData discGlobalData;
    private final ChatClient queryGeneratorToolClient;
    private final WebSearchMemoryService webSearchMemoryService;

    public WebQAService(
            DiscGlobalData discGlobalData,
            ChatClient queryGeneratorToolClient,
            WebSearchMemoryService webSearchMemoryService
    ) {
        this.queryGeneratorToolClient = queryGeneratorToolClient;
        this.webSearchMemoryService = webSearchMemoryService;
        this.discGlobalData = discGlobalData;
    }

    public List<MergedWebQAItem> getWebQAResults( List<RecentMessage> recentMessages){
        String query = generateQuery(recentMessages);
        WebQAMemory webQAMemory = webSearchMemoryService.searchExistingContent(query);

        if (webQAMemory == null) {
            return null;
        }

        List<MergedWebQAItem> results = webQAMemory.results();

        if (results == null || results.isEmpty()){
            return null;
        }

        return results;
    }

    private String generateQuery(List<RecentMessage> recentMessages) {

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(vectorDBQuery.class)
                .maxRepeatAttempts(3)
                .build();

        var queryConv = new BeanOutputConverter<>(vectorDBQuery.class);

        Map<String, Object> summarySchema = queryConv.getJsonSchemaMap();

        vectorDBQuery modelOut = queryGeneratorToolClient.prompt()
                .options(OllamaChatOptions.builder()
                        .format(summarySchema)// <- key
                        .internalToolExecutionEnabled(true)
                        .disableThinking()
                        .build())
                .tools(new WebSearchTool(webSearchMemoryService))
                .system("Create and improve a vector database query based on the User Message")
                .user(user ->
                        user.text(buildUserMessage(recentMessages))
                )
                .advisors(validation)
                .call()
                .entity(vectorDBQuery.class);

        log.info("vectorDBQuery: {}", modelOut);

        if (modelOut == null) {
            return "";
        }

        return safe(modelOut.query);
    }

    public String buildUserMessage(List<RecentMessage> recentMessages) {
        return recentMessages != null ?
                recentMessages + "\n current_user_message: " + discGlobalData.getUserMessage()
                :
                "current_user_message: " + discGlobalData.getUserMessage() ;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private record vectorDBQuery(String query) {
    }

}
