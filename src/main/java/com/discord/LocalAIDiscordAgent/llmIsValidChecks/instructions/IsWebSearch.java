package com.discord.LocalAIDiscordAgent.llmIsValidChecks.instructions;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmIsValidChecks.records.IsValidContextRecord;
import com.discord.LocalAIDiscordAgent.llmIsValidChecks.records.IsValidRecord;

import java.util.List;

public class IsWebSearch {

    private static final String SYSTEM_MESSAGE = """
            You will receive one JSON object with this structure:
            %s
            
            Apply all instructions in "instructions".
            
            Your task is to decide whether a web search is genuinely necessary based on the user_message and the retrieved_context.
            
            Treat user_message as the primary source of intent.
            Use retrieved_context only as supporting context when it helps clarify the meaning of the user_message.
            
            A web search should be used only when the user_message expresses a real need for external, factual, current, verifiable, or lookup-based information that cannot be answered reliably without searching.
            
            A web search should not be used for:
            - banter,
            - casual conversation,
            - greetings,
            - acknowledgements,
            - filler,
            - reactions,
            - encouragement,
            - opinion-free conversational continuation,
            - rhetorical questions,
            - or messages that can be answered directly without external information.
            
            Do not trigger a web search merely because the user_message is phrased as a question.
            Do not trigger a web search merely because retrieved_context contains factual content.
            Prefer false unless there is a clear and meaningful need for web search.
            
            Return only:
            - "decision" : {
                  "type" : "boolean"
                }
            """;

    private static final List<String> INSTRUCTIONS = List.of(
            "Decide whether a web search is genuinely necessary.",
            "Treat user_message as the primary source of intent.",
            "Use retrieved_context only as supporting context to clarify the user_message.",
            "Return true only when the user_message requires external, factual, current, verifiable, or lookup-based information.",
            "Return false for banter, casual conversation, greetings, acknowledgements, filler, reactions, rhetorical questions, or social chat.",
            "Return false for messages that can be answered directly without web search.",
            "Do not trigger web search simply because the message is a question.",
            "Do not trigger web search simply because retrieved_context contains facts or named entities.",
            "When uncertain, prefer false.",
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
                        null,
                        discGlobalData.getRecentMessages(),
                        null,
                        null
                )
        );
    }
}
