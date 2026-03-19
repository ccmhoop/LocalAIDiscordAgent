package com.discord.LocalAIDiscordAgent.webQA.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.Memory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.WebQAMemory;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WebQAService {

    private final ObjectMapper aiObjectMapper;
    private final DiscGlobalData discGlobalData;
    private final ChatClient queryGeneratorToolClient;
    private final WebSearchMemoryService webSearchMemoryService;
    private static final SystemMessage SYSTEM_MESSAGE = new SystemMessage("""
            You will receive one JSON object with this shape:
            {
              "instructions": [string],
              "include_date": boolean,
              "runtime_context": {
                "date": string,
                "time": string,
                "memory": object,
                "recent_messages": array,
                "user_message": string
              }
            }
            
            Apply all items in "instructions".
            Use "runtime_context" if available as supporting context.
            Produce one optimized semantic vector search query.
            Do not explain your reasoning.
            Return only the query.
            """);

    private static final List<String> INSTRUCTIONS = List.of(
            "Create a semantic vector search query based on the RuntimeContext.",
            "Enhance the query by adding relevant context, synonyms, implied intent, and clarifying terminology to improve retrieval performance.",
            "Do not repeat the user's message verbatim.",
            "Always transform the input into a richer, more searchable query that is optimized for vector similarity matching.",
            "Preserve the original meaning and intent of the user's request while improving retrieval effectiveness.",
            "Include relevant dates when the user message is time-sensitive, refers to a specific date or time period, or depends on temporal context."
    );

    private List<RecentMessage> recentMessages;
    private Memory memory;

    public WebQAService(
            DiscGlobalData discGlobalData,
            ChatClient queryGeneratorToolClient,
            WebSearchMemoryService webSearchMemoryService,
            @Qualifier("aiObjectMapper") ObjectMapper aiObjectMapper
    ) {
        this.queryGeneratorToolClient = queryGeneratorToolClient;
        this.webSearchMemoryService = webSearchMemoryService;
        this.discGlobalData = discGlobalData;
        this.aiObjectMapper = aiObjectMapper;
    }

    public List<MergedWebQAItem> getWebQAResults(
            Memory baseMemory,
            List<RecentMessage> recentMessages
    ) {
        this.recentMessages = recentMessages;
        this.memory = baseMemory;

        String query = generateQuery();
        WebQAMemory webQAMemory = webSearchMemoryService.searchExistingContent(query);

        if (webQAMemory == null) {
            return null;
        }

        List<MergedWebQAItem> results = webQAMemory.results();

        if (results == null || results.isEmpty()) {
            return null;
        }

        return results;
    }

    private String generateQuery() {

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(vectorDBQuery.class)
                .maxRepeatAttempts(3)
                .build();

        var queryConv = new BeanOutputConverter<>(vectorDBQuery.class);

        Map<String, Object> querySchema = queryConv.getJsonSchemaMap();

        Prompt prompt = buildPrompt();

        vectorDBQuery modelOut = queryGeneratorToolClient.prompt(prompt)
                .options(OllamaChatOptions.builder()
                        .format(querySchema)
                        .internalToolExecutionEnabled(true)
                        .disableThinking()
                        .build())
                .advisors(validation)
                .call()
                .entity(vectorDBQuery.class);

        log.info("vectorDBQuery: {}", modelOut);

        if (modelOut == null) {
            return "";
        }

        return safe(modelOut.query);
    }

    private Prompt buildPrompt() {
        return Prompt.builder()
                .messages(
                        SYSTEM_MESSAGE,
                        userInstructionMessage()
                )
                .build();
    }

    private UserMessage userInstructionMessage() {
        try {
            InstructionsRecord instructionsRecord = buildInstructionsRecord();
            String userInstructionJson = aiObjectMapper.writeValueAsString(instructionsRecord);
            return new UserMessage(userInstructionJson);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize system message config", e);
        }

    }

    private InstructionsRecord buildInstructionsRecord() {
        return new InstructionsRecord(
                INSTRUCTIONS,
                true,
                new RuntimeContext(
                        LocalDate.now().toString(),
                        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                        this.memory,
                        this.recentMessages,
                        discGlobalData.getUserMessage()
                )
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private record InstructionsRecord(
            List<String> instructions,
            boolean includeDateIfRelevant,
            RuntimeContext runtimeContext
    ) {
    }

    private record RuntimeContext(
            String date,
            String time,
            Memory memory,
            List<RecentMessage> recentMessages,
            String userMessage
    ) {
    }

    private record vectorDBQuery(String query) {
    }

}
