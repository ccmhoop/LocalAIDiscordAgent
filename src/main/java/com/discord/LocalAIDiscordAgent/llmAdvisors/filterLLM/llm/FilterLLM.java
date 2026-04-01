package com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.llm;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.request.FilterRequest;
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
public class FilterLLM {

    private final ChatClient llm;
    private final MapperUtils mapperUtils;
    private final DiscGlobalData discGlobalData;

    private Prompt prompt;

    public FilterLLM(
            DiscGlobalData discGlobalData,
            ChatClient filterLLMClient, MapperUtils mapperUtils
    ) {
        this.llm = filterLLMClient;
        this.discGlobalData = discGlobalData;
        this.mapperUtils = mapperUtils;
    }

    public Record call(@NonNull FilterRequest request) {
        boolean contextPresent = request.getContext() != null;
        log.info("Use LLM Filter : {}", contextPresent);
        if (!contextPresent) {
            return null;
        }
        setPrompt(request);
        log.info("LLM Filter prompt: {}", prompt);
        return callLLM(request.getOutputRecordClass());
    }

    private void setPrompt(FilterRequest request) {
        String systemMessage = buildSystemMessageJson(request);
        this.prompt = Prompt.builder()
                .messages(
                        new SystemMessage(systemMessage),
                        new UserMessage(discGlobalData.getUserMessage())
                )
                .build();
    }

    private String buildSystemMessageJson(FilterRequest request) {
        String jsonContextString = mapperUtils.valuesToString(request.getContext());
        return request.getSystemMessage().formatted(jsonContextString);
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
