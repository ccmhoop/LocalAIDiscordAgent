package com.discord.LocalAIDiscordAgent.llmIsValidChecks.instructions;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmIsValidChecks.records.IsValidContextRecord;
import com.discord.LocalAIDiscordAgent.llmIsValidChecks.records.IsValidRecord;

import java.util.List;

public class IsChatMemoryValid {

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
            Return only:
            - "decision" : {
                  "type" : "boolean"
                }
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
            """
            Return only:
            - "decision" : {
                  "type" : "boolean"
                }
            """
    );

    protected static IsValidRecord getInstructions(DiscGlobalData discGlobalData) {
        return new IsValidRecord(
                SYSTEM_MESSAGE,
                INSTRUCTIONS,
                new IsValidContextRecord(
                        discGlobalData.getGroupChatMemory(),
                        discGlobalData.getLongTermMemoryData(),
                        discGlobalData.getRecentMessages(),
                        null,
                        null
                )
        );
    }

}
