package com.discord.LocalAIDiscordAgent.resolverLLM.llm;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;

import com.discord.LocalAIDiscordAgent.resolverLLM.records.ResolverLMMContextRecord;
import com.discord.LocalAIDiscordAgent.resolverLLM.records.ResolverLLMPayloadRecord;
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
public class ResolverLLM {

    private final ChatClient llmClient;
    private final MapperUtils mapperUtils;
    private final DiscGlobalData discGlobalData;

    private ResolverLMMContextRecord payloadContextData;
    private String systemMsg;
    private Prompt prompt;


    public ResolverLLM(
            MapperUtils mapperUtils,
            ChatClient resolverLLMClient,
            DiscGlobalData discGlobalData
    ) {
        this.discGlobalData = discGlobalData;
        this.llmClient = resolverLLMClient;
        this.mapperUtils = mapperUtils;
    }

    public boolean call(@NonNull ResolverLLMPayloadRecord payload) {
        this.payloadContextData = payload.payloadContext();
        this.systemMsg = payload.systemMsg();
        if (payload.payloadContext() != null) {
            buildSystemMessageJson();
        }
        setPrompt();
        log.info("LLM resolver prompt: {}", prompt);
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
