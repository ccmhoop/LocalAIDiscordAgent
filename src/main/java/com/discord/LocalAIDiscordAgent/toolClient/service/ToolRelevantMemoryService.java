package com.discord.LocalAIDiscordAgent.toolClient.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;

import com.discord.LocalAIDiscordAgent.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
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
public class ToolRelevantMemoryService {

    private final DiscGlobalData discGlobalData;
    private final MapperUtils mapperUtils;
    private final ChatClient queryGeneratorToolClient;

    private static final String SYSTEM_MESSAGE = """
            You will receive one JSON object with this structure:
            
            %s
            
            Apply all instructions in "instructions".
            
            Your task is to decide whether the retrieved_context is relevant to the user_message.
            
            Consider the retrieved_context relevant only if it contains information that would meaningfully help answer, explain, support, or clarify the user_message.
            
            Consider the retrieved_context not relevant if it is:
            - off-topic,
            - only loosely related,
            - based only on superficial keyword overlap,
            - too generic to be useful,
            - misleading or contextually mismatched.
            
            Use semantic meaning, topic alignment, intent, entities, and context when making the decision.
            If the relevance is weak, uncertain, or ambiguous, return false.
            
            Do not explain your reasoning.
            Return only true or false.
            """;

    private static final List<String> INSTRUCTIONS = List.of(
            "Determine whether the retrieved_context is relevant to the user_message.",
            "Treat the user_message as the primary source of intent.",
            "Mark the result as true only if the retrieved_context contains information that directly helps answer, explain, support, or clarify the user_message.",
            "Mark the result as false if the retrieved_context is unrelated, weakly related, off-topic, misleading, or too generic to be useful.",
            "Do not treat vague keyword overlap alone as sufficient evidence of relevance.",
            "Use semantic meaning, topic alignment, entities, intent, and context when deciding relevance.",
            "Prefer false when relevance is uncertain, weak, or ambiguous.",
            "Ignore conversational filler and focus only on whether the retrieved_context is meaningfully useful for the user_message.",
            "Return only the relevance decision."
    );

    public ToolRelevantMemoryService(
            DiscGlobalData discGlobalData,
            ChatClient queryGeneratorToolClient,
            MapperUtils mapperUtils
    ) {
        this.queryGeneratorToolClient = queryGeneratorToolClient;
        this.discGlobalData = discGlobalData;
        this.mapperUtils = mapperUtils;
    }

    public boolean isMemoryRelevant() {
        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(RetrievalDecision.class)
                .maxRepeatAttempts(3)
                .build();

        var converter = new BeanOutputConverter<>(RetrievalDecision.class);

        Prompt prompt = buildPrompt();

        log.info("prompt: {}", prompt);

        RetrievalDecision modelOut = queryGeneratorToolClient.prompt(prompt)
                .options(OllamaChatOptions.builder()
                        .format(converter.getJsonSchema())
                        .build())
                .advisors(validation)
                .call()
                .entity(RetrievalDecision.class);

        log.info("is memory relevant: {}", modelOut);

        if (modelOut == null) {
            return true;
        }

        return modelOut.retrievedDataRelevant;
    }

    private SystemMessage buildSystemMessage(InstructionsRecord instructionsRecord) {
        String jsonSchema = mapperUtils.generateSchema(instructionsRecord);
        return new SystemMessage(
                SYSTEM_MESSAGE.formatted(jsonSchema)
        );
    }

    private Prompt buildPrompt() {
        InstructionsRecord instructionsRecord = buildInstructionsRecord();
        return Prompt.builder()
                .messages(
                        buildSystemMessage(instructionsRecord),
                        userInstructionMessage(instructionsRecord)
                )
                .build();
    }

    private UserMessage userInstructionMessage(InstructionsRecord instructionsRecord) {
            return new UserMessage(
                    mapperUtils.recordToString(instructionsRecord)
            );
    }

    private InstructionsRecord buildInstructionsRecord() {
        return new InstructionsRecord(
                INSTRUCTIONS,
                new RetrievedContextRecord(
                        discGlobalData.getGroupChatMemory(),
                        discGlobalData.getLongTermMemoryData(),
                        discGlobalData.getRecentMessages()),
                discGlobalData.getUserMessage()
        );
    }

    public record InstructionsRecord(
            List<String> instructions,
            RetrievedContextRecord retrievedContext,
            String userMessage
    ) {
    }

    public record RetrievedContextRecord(
            GroupMemory groupMemory,
            List<LongTermMemoryData> longTermMemory,
            List<RecentMessage> recentMessages
    ) {
    }

    public record RetrievalDecision(
            boolean retrievedDataRelevant
    ) {
    }
}
