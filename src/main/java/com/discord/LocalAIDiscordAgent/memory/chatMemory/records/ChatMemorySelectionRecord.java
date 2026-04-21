package com.discord.LocalAIDiscordAgent.memory.chatMemory.records;


import com.discord.LocalAIDiscordAgent.memory.chatMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;

import java.util.List;

public record ChatMemorySelectionRecord(
        List<LongTermMemoryData> longTermMemory,
        ChatMessagesSelection chatMemory
) {
    public record ChatMessagesSelection(
            List<MessagePair> retrievedMemory
            ){
        public record MessagePair (int id, String timeStamp, List<ChatMessage> messages, ChatMessage message) {
            public record ChatMessage(String User, String Assistant) {
            }
        }

    }
}
