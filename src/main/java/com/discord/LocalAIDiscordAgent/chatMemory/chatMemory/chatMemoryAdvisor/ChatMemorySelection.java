package com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.chatMemoryAdvisor;

import java.util.List;

public record ChatMemorySelection(
        List<Integer> ids,
        boolean includeLongTermMemory
) {
}