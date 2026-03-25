package com.discord.LocalAIDiscordAgent.llmChatMemoryFilter.instructions;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmChatMemoryFilter.records.ChatMemoryContext;
import com.discord.LocalAIDiscordAgent.llmChatMemoryFilter.records.ChatMemoryFilter;

import java.util.List;

public class ReduceChatMemory {

    private static final String SYSTEM_MESSAGE = """
            You will receive one JSON object with this structure:
            %s
            
            Apply all instructions in "instructions".
            
            Your task is to reduce the chat memory so it includes only the most important and useful information.
            - Only keep chat memory that is relevant to the user_message.
            - If a summary is available, use it to identify the most important chat memory.
            - Never return the user_message, only the relevant chat memory.
            - Never generate an assistant chat response.
           
            """;

    private static final List<String> INSTRUCTIONS = List.of(
            "Reduce the chat memory to the minimum set of high-value information needed for the user_message.",
            "Use the user_message as the primary relevance anchor.",
            "Use the summary, if available, to identify the most important themes, facts, and context.",
            "Keep only memory that is directly relevant, supportive, or necessary for understanding the user_message.",
            "Merge overlapping memory conceptually by keeping only the clearest and most informative content.",
            "Never return the user_message, only the relevant chat memory.",
            "Never generate an assistant chat response",
            "Return only the reduced chat memory."
    );

    protected static ChatMemoryFilter getInstructions(DiscGlobalData discGlobalData, String summary) {
        return new ChatMemoryFilter(
                SYSTEM_MESSAGE,
                INSTRUCTIONS,
                new ChatMemoryContext(
                        summary,
                        null,
                        null,
                        discGlobalData.getRecentMessages()
                )
        );
    }

}
