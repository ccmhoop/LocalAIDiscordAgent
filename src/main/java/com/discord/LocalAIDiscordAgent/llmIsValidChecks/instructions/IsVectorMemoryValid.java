package com.discord.LocalAIDiscordAgent.llmIsValidChecks.instructions;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.llmIsValidChecks.records.IsValidContextRecord;
import com.discord.LocalAIDiscordAgent.llmIsValidChecks.records.IsValidRecord;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;

import java.util.List;

public class IsVectorMemoryValid {
    private static final String SYSTEM_MESSAGE = """
            You will receive one JSON object with this structure:
            %s
            
            Apply all instructions in "instructions".
            
            Your task is to decide whether the retrieved_context can be used to answer the user_message.
            
            Return true only when the retrieved_context contains information that is sufficiently relevant and useful to answer, support, or clarify the user_message.
            
            Return false when the retrieved_context is:
            - unrelated,
            - only weakly related,
            - too generic,
            - incomplete for answering,
            - contextually mismatched,
            - or connected only by keyword overlap without real semantic usefulness.
            
            Base the decision on semantic relevance, topical alignment, user intent, entity alignment, and practical usefulness for answering the user_message.
            
            If the usefulness of the retrieved_context is uncertain, weak, or ambiguous, return false.
            
            Do not explain your reasoning.
            Return only true or false.
            """;

    private static final List<String> INSTRUCTIONS = List.of(
            "Determine whether the retrieved_context can be used to answer the user_message.",
            "Treat the user_message as the primary source of intent.",
            "Return true only if the retrieved_context contains information that directly answers, supports, or materially helps answer the user_message.",
            "Return false if the retrieved_context is off-topic, only loosely related, too generic, incomplete for answering, or based only on superficial keyword overlap.",
            "Use semantic meaning, topic alignment, entities, intent, and context when making the decision.",
            "Do not assume relevance from shared words alone.",
            "Prefer false when usefulness is weak, uncertain, or ambiguous.",
            "Ignore conversational filler and judge only whether the retrieved_context is actually usable for answering the user_message.",
            "Return only the boolean decision."
    );

    protected static IsValidRecord getInstructions(List<MergedWebQAItem> vectorDBMemory, RecentMessage lastAssistantMsg) {
        return new IsValidRecord(
                SYSTEM_MESSAGE,
                INSTRUCTIONS,
                new IsValidContextRecord(
                        null,
                        null,
                        null,
                        vectorDBMemory,
                        null
                )
        );
    }
}
