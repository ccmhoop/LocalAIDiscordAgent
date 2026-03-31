package com.discord.LocalAIDiscordAgent.structuredLLM.llm;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.structuredLLM.records.StructuredLLMContextRecord;
import com.discord.LocalAIDiscordAgent.structuredLLM.records.StructuredLLMPayloadRecord;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StructuredLLM {

    private final ChatClient llm;
    private final DiscGlobalData discGlobalData;

    public StructuredLLM(
            DiscGlobalData discGlobalData,
            ChatClient structuredLLMClient
    ) {
        this.discGlobalData = discGlobalData;
        this.llm = structuredLLMClient;
    }

    public Record call(@NonNull StructuredLLMPayloadRecord payload, Class<? extends Record> outputType) {

        StructuredLLMPayload instructions = new StructuredLLMPayload(
                payload.instructions(),
                payload.context(),
                discGlobalData.getUserMessage()
        );

        return callLLM(
                payload.systemMsg(),
                instructions,
                outputType
        );
    }

    private <T extends Record> T callLLM(
            String systemTemplate,
            StructuredLLMPayload payload,
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

        return llm.prompt()
                .system(systemTemplate)
                .user(payload.userMessage())
                .advisors(validation)
                .call()
                .entity(outputType);
    }

    public record StructuredLLMPayload(
            List<String> instructions,
            StructuredLLMContextRecord retrievedContext,
            String userMessage
    ) {}

}
