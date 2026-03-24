package com.discord.LocalAIDiscordAgent.llmQueryGenerator.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.records.QueryContextRecord;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.records.QueryGeneratorRecord;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LLMClientQueryGeneratorService {

    private final MapperUtils mapperUtils;
    private final DiscGlobalData discGlobalData;
    private final ChatClient queryGeneratorToolClient;

    private QueryInstructions queryInstructions;
    private QueryContextRecord queryContext;
    private List<String> instructions;
    private String systemMsg;
    private Prompt prompt;

    public LLMClientQueryGeneratorService(MapperUtils mapperUtils, DiscGlobalData discGlobalData, ChatClient queryGeneratorToolClient) {
        this.mapperUtils = mapperUtils;
        this.discGlobalData = discGlobalData;
        this.queryGeneratorToolClient = queryGeneratorToolClient;
    }

    public String generateQuery(@NonNull QueryGeneratorRecord queryGeneratorRecord){
        this.instructions = queryGeneratorRecord.instructions();
        this.queryContext = queryGeneratorRecord.context();
        this.systemMsg = queryGeneratorRecord.systemMsg();
        setQueryGeneratorInstructions();
        setPrompt();
        log.debug("LLM generate query prompt: {}", prompt);
        return callLLM();
    }

    private void setQueryGeneratorInstructions() {
        this.queryInstructions = new QueryInstructions(
                instructions,
                queryContext,
                discGlobalData.getUserMessage()
        );
    }

    public record QueryInstructions(
            List<String> instructions,
            QueryContextRecord context,
            String userMessage
    ) {}

    private void setPrompt() {
        this.prompt = Prompt.builder()
                .messages(
                        buildSystemMessageJson(),
                        buildUserMessageJson()
                )
                .build();
    }

    private SystemMessage buildSystemMessageJson() {
        String objectJsonSchema = mapperUtils.objectTypeTostring(queryInstructions);
        this.systemMsg = this.systemMsg.formatted(objectJsonSchema);
        return new SystemMessage(this.systemMsg);
    }

    private UserMessage buildUserMessageJson() {
        String userMessage = mapperUtils.valuesToString(queryInstructions);
        return new UserMessage(userMessage);
    }

    private String callLLM() {
        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(LLMQueryGenerationRecord.class)
                .maxRepeatAttempts(3)
                .build();

        var converter = new BeanOutputConverter<>(LLMQueryGenerationRecord.class);

        LLMQueryGenerationRecord llmResponse = queryGeneratorToolClient.prompt(this.prompt)
                .options(OllamaChatOptions.builder()
                        .format(converter.getJsonSchema())
                        .build())
                .advisors(validation)
                .call()
                .entity(LLMQueryGenerationRecord.class);

        if (llmResponse == null || llmResponse.query().isBlank()) {
            return null;
        }

        return llmResponse.query();
    }

    public record LLMQueryGenerationRecord(
            String query
    ){}
}
