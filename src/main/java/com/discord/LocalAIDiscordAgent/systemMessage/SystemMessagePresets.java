package com.discord.LocalAIDiscordAgent.systemMessage;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.*;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.WebQAMemory;

import java.util.List;

public final class SystemMessagePresets {

    private SystemMessagePresets() {
    }

    public static SystemMessageConfig qwenFriendlyDefault() {
        return SystemMessageFactory.defaultConfig();
    }


    public static SystemMessageConfig withMessageMemory(
            SystemMessageConfig base,
            UserProfile userProfile,
            Memory memory,
            WebQAMemory webSearchMemory,
            List<RecentMessage> recentMemory,
            GroupMemory groupMemory,
            String userMessage
    ) {
        return new SystemMessageConfig(
                base.systemContract(),
                base.personality(),
                base.rules(),
                base.style(),
                base.decisionPolicy(),
                base.technicalResponsePolicy(),
                base.memoryRules(),
                base.antiRepetitionRules(),
                userProfile,
                memory,
                webSearchMemory,
                recentMemory,
                groupMemory,
                userMessage,
                base.responseContract()
        );
    }
}