package com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.records;


import com.discord.LocalAIDiscordAgent.llmMemory.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;

import java.util.List;

public record FilterLLMContextRecord(
        List<LongTermMemoryData> longTermMemory,
        ChatMessagesRecord chatMemory
) {
    public record ChatMessagesRecord(
            List<MessagePair> retrievedMemory
            ){
        public record MessagePair (int id, String timeStamp, List<ChatMessage> messages, ChatMessage message) {
            public record ChatMessage(String User, String Assistant) {
            }
        }

    }
}
