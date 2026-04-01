package com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.llm;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;

import com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.request.BooleanLLMRequest;
import com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.records.BooLeanLMMContextRecord;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BooleanLLM {

    private final ChatClient llmClient;
    private final MapperUtils mapperUtils;
    private final DiscGlobalData discGlobalData;

    private BooLeanLMMContextRecord payloadContextData;
    private String systemMsg;
    private Prompt prompt;

    public BooleanLLM(
            MapperUtils mapperUtils,
            ChatClient booleanLLMClient,
            DiscGlobalData discGlobalData
    ) {
        this.discGlobalData = discGlobalData;
        this.llmClient = booleanLLMClient;
        this.mapperUtils = mapperUtils;
    }

    public <T extends BooleanLLMRequest> boolean call(@NonNull T payload) {
        this.payloadContextData = payload.getContext();
        this.systemMsg = payload.getSystemMessage();
        if (this.payloadContextData != null) {
            buildSystemMessageJson();
        }
        setPrompt();
        log.debug("LLM resolver prompt: {}", prompt);
        return clientCallLLM();
    }

    private void setPrompt() {
        this.prompt = Prompt.builder()
                .messages(
                        new SystemMessage(this.systemMsg),
                        new UserMessage(discGlobalData.getUserMessage())
                )
                .build();
    }

    private void buildSystemMessageJson() {
        String jsonContextString = mapperUtils.valuesToString(payloadContextData);
        this.systemMsg = this.systemMsg.formatted(jsonContextString);
    }

    private boolean clientCallLLM() {
        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(LLMDecision.class)
                .maxRepeatAttempts(3)
                .build();

        var converter = new BeanOutputConverter<>(LLMDecision.class);

        LLMDecision llmResponse = llmClient.prompt(this.prompt)
                .options(OllamaChatOptions.builder()
                        .format(converter.getJsonSchema())
                        .build())
                .advisors(validation)
                .call()
                .entity(LLMDecision.class);

        if (llmResponse == null) {
            return true;
        }

        return llmResponse.decision;
    }

    public record LLMDecision(
            Boolean decision
    ) {
    }
}
