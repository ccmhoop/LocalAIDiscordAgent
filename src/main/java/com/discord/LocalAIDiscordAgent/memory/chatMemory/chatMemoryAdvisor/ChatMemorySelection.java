package com.discord.LocalAIDiscordAgent.memory.chatMemory.chatMemoryAdvisor;

import java.util.List;

public record ChatMemorySelection(
        List<Integer> ids,
        boolean includeLongTermMemory
) {
}