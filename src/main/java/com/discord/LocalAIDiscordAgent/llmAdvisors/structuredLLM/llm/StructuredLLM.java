package com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.llm;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.request.StructuredLLMRequest;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class StructuredLLM {

    private final ChatClient llm;
    private final MapperUtils mapperUtils;
    private final DiscGlobalData discGlobalData;

    private String systemMsg;
    private Prompt prompt;

    public StructuredLLM(
            MapperUtils mapperUtils, DiscGlobalData discGlobalData,
            ChatClient structuredLLMClient
    ) {
        this.mapperUtils = mapperUtils;
        this.discGlobalData = discGlobalData;
        this.llm = structuredLLMClient;
    }

    public Record call(@NonNull StructuredLLMRequest request) {
        this.systemMsg = request.getSystemMessage();
        if (request.getContext() != null) {
            buildSystemMessageJson(request);
        }
        setPrompt();
        return callLLM(request.getOutputRecordClass());
    }

    private void setPrompt() {
        this.prompt = Prompt.builder()
                .messages(
                        new SystemMessage(this.systemMsg),
                        new UserMessage(discGlobalData.getUserMessage())
                )
                .build();
    }

    private void buildSystemMessageJson(StructuredLLMRequest request) {
        String jsonContextString = mapperUtils.valuesToString(request.getContext());
        this.systemMsg = this.systemMsg.formatted(jsonContextString);
    }

    private <T extends Record> T callLLM(Class<T> outputType) {
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

        return llm.prompt(this.prompt)
                .advisors(validation)
                .call()
                .entity(outputType);
    }

}
