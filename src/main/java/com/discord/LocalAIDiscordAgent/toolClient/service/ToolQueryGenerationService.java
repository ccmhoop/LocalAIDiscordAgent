package com.discord.LocalAIDiscordAgent.toolClient.service;


import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.Memory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class ToolQueryGenerationService {

    private final DiscGlobalData discGlobalData;
    private final ChatClient queryGeneratorToolClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<RecentMessage> recentMessages;

    private static final SystemMessage SYSTEM_MESSAGE = new SystemMessage("""
        You will receive one JSON object with this structure:
        {
          "instructions": [string],
          "include_date": boolean,
          "runtime_context": {
            "date": string,
            "time": string,
            "recent_messages": array,
            "user_message": string
          }
        }

        Apply all instructions in "instructions".

        Your first task is to decide whether the user_message requires semantic retrieval.
        Retrieval is required only when the user_message:
        - asks for information,
        - depends on prior conversation context or stored memory,
        - refers to facts, entities, events, or documents,
        - would benefit from semantic search to answer well.

        Retrieval is not required for:
        - greetings,
        - acknowledgements,
        - short reactions,
        - conversational filler,
        - social banter,
        - encouragement,
        - generic follow-up phrases,
        - transitions that do not introduce an information need.

        Treat runtime_context.user_message as the primary source of intent.
        Use other runtime_context fields only when they are relevant and improve retrieval quality.
        Ignore irrelevant or distracting context.

        If include_date is true, include relevant temporal context only when the request is time-sensitive.

        If retrieval is needed, generate exactly one concise, information-rich semantic vector search query optimized for retrieval.
        If retrieval is not needed, return an empty string.

        Do not explain your reasoning.
        Return only the final query text or an empty string.
        """);

    private static final List<String> INSTRUCTIONS = List.of(
            "Determine first whether the user_message requires semantic retrieval.",
            "Generate a semantic vector search query only when the user_message expresses an information need, references prior knowledge, or would benefit from memory or document retrieval.",
            "Do not generate a query for casual conversation, acknowledgements, greetings, reactions, filler, conversational transitions, encouragement, or social banter.",
            "If the user_message does not require retrieval, return an empty string.",
            "Treat the user_message as the primary source of intent.",
            "Use recent_messages and other runtime_context fields only when they are relevant to the user_message and improve retrieval quality.",
            "Ignore irrelevant, weak, or distracting context.",
            "When retrieval is needed, expand the query with relevant context, synonyms, implied intent, and clarifying terminology.",
            "Do not copy the user_message verbatim unless it is already precise, complete, and retrieval-optimized.",
            "Preserve the original meaning, intent, key entities, and important constraints from the user_message.",
            "Include relevant dates, time periods, recency cues, or temporal qualifiers when the request is time-sensitive.",
            "Return only the final optimized query or an empty string."
    );

    public ToolQueryGenerationService(
            DiscGlobalData discGlobalData,
            ChatClient queryGeneratorToolClient
    ) {
        this.queryGeneratorToolClient = queryGeneratorToolClient;
        this.discGlobalData = discGlobalData;
    }

    public String generateQuery(List<RecentMessage> recentMessages) {
        this.recentMessages = recentMessages;

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(RetrievalDecision.class)
                .maxRepeatAttempts(3)
                .build();

        var converter = new BeanOutputConverter<>(RetrievalDecision.class);

        Prompt prompt = buildPrompt();

        RetrievalDecision modelOut = queryGeneratorToolClient.prompt(prompt)
                .options(OllamaChatOptions.builder()
                        .format(converter.getJsonSchema())
                        .build())
                .advisors(validation)
                .call()
                .entity(RetrievalDecision.class);

        log.info("vectorDBQuery: {}", modelOut);

        if (modelOut == null || !modelOut.requiresRetrieval()) {
            return "";
        }

        return safe(modelOut.query);
    }

    private Prompt buildPrompt() {
        return Prompt.builder()
                .messages(
                        SYSTEM_MESSAGE,
                        userInstructionMessage()
                )
                .build();
    }

    private UserMessage userInstructionMessage() {
        try {
            InstructionsRecord instructionsRecord = buildInstructionsRecord();
            String userInstructionJson = objectMapper.writeValueAsString(instructionsRecord);
            return new UserMessage(userInstructionJson);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize system message config", e);
        }

    }

    private InstructionsRecord buildInstructionsRecord() {
        return new InstructionsRecord(
                INSTRUCTIONS,
                true,
                new RuntimeContext(
                        LocalDate.now().toString(),
                        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                        null,
                        this.recentMessages,
                        discGlobalData.getUserMessage()
                )
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private record InstructionsRecord(
            List<String> instructions,
            boolean includeDateIfRelevant,
            RuntimeContext runtimeContext
    ) {}

    private record RuntimeContext(
            String date,
            String time,
            Memory memory,
            List<RecentMessage> recentMessages,
            String userMessage
    ) {}

    public record RetrievalDecision(
            boolean requiresRetrieval,
            String query
    ) {}

}