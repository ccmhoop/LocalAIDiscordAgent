package com.discord.LocalAIDiscordAgent.textLLM.payload;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.textLLM.records.TextLLMContextRecord;
import com.discord.LocalAIDiscordAgent.textLLM.records.TextLLMPayloadRecord;

import java.util.List;

public class TextLLMVectorQueryPayload {

    private static final String SYSTEM_MESSAGE = """        
            Your task is to optimize the user_message for semantic retrieval, create a information-rich semantic vector search query optimized for retrieval.
            
            Do not explain your reasoning.
            """;

    private static final List<String> INSTRUCTIONS = List.of(
            "Treat the user_message as the primary source of intent.",
            "Do not copy the user_message verbatim unless it is already precise, complete, and retrieval-optimized.",
            "Preserve the original meaning, intent, key entities, and important constraints from the user_message."
    );

    public static TextLLMPayloadRecord getPayload(DiscGlobalData discGlobalData) {
        return new TextLLMPayloadRecord(
                SYSTEM_MESSAGE,
                INSTRUCTIONS,
                new TextLLMContextRecord(
                        discGlobalData.getLongTermMemoryData(),
                        discGlobalData.getLastAssistantMsg()
                )
        );
    }

}
