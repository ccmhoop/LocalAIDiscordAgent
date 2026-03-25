package com.discord.LocalAIDiscordAgent.llmQueryGenerator.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.records.QueryContextRecord;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.records.QueryGeneratorRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
public class LLMClientQueryGeneratorService {

    private final DiscGlobalData discGlobalData;
    private final ChatClient queryGeneratorToolClient;

    public LLMClientQueryGeneratorService(
            DiscGlobalData discGlobalData,
            ChatClient queryGeneratorToolClient
    ) {
        this.discGlobalData = discGlobalData;
        this.queryGeneratorToolClient = queryGeneratorToolClient;
    }

    public Record generateQuery(@NonNull QueryGeneratorRecord queryGeneratorRecord) {
        Class<? extends Record> outputType =
                queryGeneratorRecord.context() == null
                        ? ImagePromptOutput.class
                        : LLMQueryGenerationRecord.class;

        QueryInstructions payload = new QueryInstructions(
                queryGeneratorRecord.instructions(),
                queryGeneratorRecord.context(),
                discGlobalData.getUserMessage()
        );

        return callLLM(
                queryGeneratorRecord.systemMsg(),
                payload,
                outputType
        );
    }

    private <T extends Record> T callLLM(
            String systemTemplate,
            QueryInstructions payload,
            Class<T> outputType
    ) {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .build();

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(outputType)
                .objectMapper(mapper)
                .maxRepeatAttempts(3)
                .build();

        return queryGeneratorToolClient.prompt()
                .system(systemTemplate)
                .user(payload.userMessage())
                .advisors(validation)
                .call()
                .entity(outputType);
    }

    public record QueryInstructions(
            List<String> instructions,
            QueryContextRecord retrievedContext,
            String userMessage
    ) {}

    public record LLMQueryGenerationRecord(
            @JsonProperty(required = true, value = "query")
            String query
    ) {}

    public record ImagePromptOutput(
            @JsonProperty(required = true, value = "positivePrompt")
            String positivePrompt,
            @JsonProperty(required = true, value = "negativePrompt")
            String negativePrompt,
            @JsonProperty(required = true, value = "pixelWidth")
            int pixelWidth,
            @JsonProperty(required = true, value = "pixelHeight")
            int pixelHeight

    ) {}
}