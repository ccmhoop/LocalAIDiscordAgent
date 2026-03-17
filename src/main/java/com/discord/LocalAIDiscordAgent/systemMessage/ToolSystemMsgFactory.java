package com.discord.LocalAIDiscordAgent.systemMessage;

import com.discord.LocalAIDiscordAgent.systemMessage.records.ToolSystemMsgRecords;
import com.discord.LocalAIDiscordAgent.systemMessage.records.ToolSystemMsgRecords.*;
import com.discord.LocalAIDiscordAgent.systemMessage.records.ToolSystemMsgRecords.ToolCall.AllowedOnlyWhen;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;


public class ToolSystemMsgFactory {

    private final ObjectMapper objectMapper;

    public ToolSystemMsgFactory(@Qualifier("aiObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildToolSystemMsgJson(ToolSystemMsgRecords config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize system message config", e);
        }
    }

    public String buildDefaultSystemMessage() {
        return buildToolSystemMsgJson(defaultToolSystemConfig());
    }

    public static ToolSystemMsgRecords defaultToolSystemConfig() {
        return new ToolSystemMsgRecords(
                List.of(
                        "receive_current_user_message",
                        "receive_retrieved_context",
                        "evaluate_context_sufficiency",
                        "if_context_sufficient_answer_directly",
                        "if_context_insufficient_call_tool",
                        "use_tool_result_to_answer"
                ),
                List.of(
                        new AvailableTools(
                                "web_search",
                                "Search the web for current or missing information",
                                Map.of("query", "string")
                        )
                ),
                new ToolUsePolicy(
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        "Context is sufficient when it contains the facts needed to answer the current user message accurately, directly, and without meaningful guessing.",
                        "If retrieved context can answer the question accurately and completely, do not call a tool.",
                        List.of(
                                "retrieved context is missing key facts",
                                "retrieved context is ambiguous, incomplete, stale, or conflicting",
                                "the user explicitly asks to search, verify, browse, check, or look up",
                                "the answer depends on current external information",
                                "the answer requires exact calculation",
                                "a specific page, file, or URL must be read"
                        ),
                        List.of(
                                "retrieved context already answers the question",
                                "recent messages already resolve the ambiguity",
                                "memory or retrieved documents provide the required facts",
                                "the tool call would only duplicate available information"
                        ),
                        true,
                        true,
                        "answer_directly_from_retrieved_context",
                        "emit_tool_call"
                ),
                new AssistantDecision(
                        true,
                        List.of(
                                "current_user_message",
                                "recent_messages",
                                "memory_summary",
                                "memory_facts",
                                "retrieved_documents"
                        ),
                        new AssistantDecision.FieldsRecord(
                                new FieldTypes(
                                        "boolean",
                                        null,
                                        true
                                ),
                                new FieldTypes(
                                        "string",
                                        null,
                                        true
                                ),
                                new FieldTypes(
                                        "string",
                                        List.of(
                                                "answer_directly",
                                                "call_tool"
                                        ),
                                        true
                                )

                        )
                ),
                new ToolCall(
                        new AllowedOnlyWhen(
                                "call_tool",
                                false
                        ),
                        new ToolCall.FieldsRecord(
                                new FieldTypes(
                                        "string",
                                        null,
                                        true
                                ),
                                new FieldTypes(
                                        "object",
                                        null,
                                        true
                                )
                        )
                ),
                new FinalAnswer(
                        new FinalAnswer.FieldsRecord(
                                new FieldTypes(
                                        "string",
                                        null,
                                        true
                                ),
                                new FieldTypes(
                                        "string",
                                        List.of(
                                                "retrieved_context", "tool_result",
                                                "retrieved_context_and_tool_result"
                                        ),
                                        true
                                ),
                                new FieldTypes(
                                        "boolean",
                                        null,
                                        true
                                )

                        )
                ),
                null
        );
    }

}
