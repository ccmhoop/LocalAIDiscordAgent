package com.discord.LocalAIDiscordAgent.llmIsValidChecks.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;

import com.discord.LocalAIDiscordAgent.llmIsValidChecks.records.IsValidContextRecord;
import com.discord.LocalAIDiscordAgent.llmIsValidChecks.records.IsValidRecord;
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

import java.util.*;

@Slf4j
@Service
public class LLMClientIsValidService {

    private final MapperUtils mapperUtils;
    private final DiscGlobalData discGlobalData;
    private final ChatClient llmIsValidClient;

    private InstructionsRecord LLMInstructionsToCheck;
    private IsValidContextRecord isValidContextRecord;
    private List<String> instructions;
    private String systemMsg;
    private Prompt prompt;


    public LLMClientIsValidService(
            MapperUtils mapperUtils,
            DiscGlobalData discGlobalData,
            ChatClient isValidToolClient
    ) {
        this.llmIsValidClient = isValidToolClient;
        this.discGlobalData = discGlobalData;
        this.mapperUtils = mapperUtils;
    }

    public boolean preformCheck(@NonNull IsValidRecord instructionRecord) {
        this.isValidContextRecord = instructionRecord.dataToCheck();
        this.instructions = instructionRecord.instruction();
        this.systemMsg = instructionRecord.systemMsg();
        setInstructionsRecord();
        setPrompt();
        log.debug("LLM is valid prompt: {}", prompt);
        return callLLM();
    }

    private void setInstructionsRecord() {
        this.LLMInstructionsToCheck = new InstructionsRecord(
                instructions,
                isValidContextRecord,
                discGlobalData.getUserMessage()
        );
    }

    public record InstructionsRecord(
            List<String> instructions,
            IsValidContextRecord isValidContextRecord,
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
        String objectJsonSchema = mapperUtils.objectTypeTostring(LLMInstructionsToCheck);
        this.systemMsg = this.systemMsg.formatted(objectJsonSchema);
        return new SystemMessage(this.systemMsg);
    }

    private UserMessage buildUserMessageJson() {
        String userMessage = mapperUtils.valuesToString(LLMInstructionsToCheck);
        return new UserMessage(userMessage);
    }

    private boolean callLLM() {
        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(LLMDecision.class)
                .maxRepeatAttempts(3)
                .build();

        var converter = new BeanOutputConverter<>(LLMDecision.class);

        LLMDecision llmResponse = llmIsValidClient.prompt(this.prompt)
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
            boolean decision
    ) {
    }
}
