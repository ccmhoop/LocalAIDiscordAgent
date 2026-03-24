package com.discord.LocalAIDiscordAgent.llmQueryGenerator.instructions;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.records.QueryContextRecord;
import com.discord.LocalAIDiscordAgent.llmQueryGenerator.records.QueryGeneratorRecord;

import java.util.List;

public class QueryVectorMemory {

    private static final String SYSTEM_MESSAGE = """
            You will receive one JSON object with this structure:
            %s
            
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
            """;

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

    protected static QueryGeneratorRecord getInstructions(DiscGlobalData discGlobalData) {
        return new QueryGeneratorRecord(
                SYSTEM_MESSAGE,
                INSTRUCTIONS,
                new QueryContextRecord(
                        discGlobalData.getLongTermMemoryData(),
                        discGlobalData.getLastAssistantMsg()
                )
        );
    }

}
