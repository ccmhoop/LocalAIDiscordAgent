package com.discord.LocalAIDiscordAgent.llmResolvers.structuredLLM.llm;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmResolvers.structuredLLM.request.StructuredLLMRequest;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;


@Component
public class StructuredLLM {

    private final ChatClient llm;
    private final DiscGlobalData discGlobalData;

    private Prompt prompt;

    public StructuredLLM(
            DiscGlobalData discGlobalData,
            ChatClient structuredLLMClient
    ) {
        this.discGlobalData = discGlobalData;
        this.llm = structuredLLMClient;
    }


    public Record call(@NonNull StructuredLLMRequest request) {
        setPrompt(request);
        return callLLM(request.getOutputRecordClass());
    }

    private void setPrompt(StructuredLLMRequest request) {
        this.prompt = Prompt.builder()
                .messages(
                        new SystemMessage(request.getSystemMessage()),
                        new UserMessage(discGlobalData.getUserMessage())
                )
                .build();
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
