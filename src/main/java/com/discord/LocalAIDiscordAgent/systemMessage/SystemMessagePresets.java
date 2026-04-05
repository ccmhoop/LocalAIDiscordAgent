package com.discord.LocalAIDiscordAgent.systemMessage;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.*;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;

import java.util.List;

public final class SystemMessagePresets {

    private SystemMessagePresets() {}

    public static SystemMessageConfig qwenFriendlyDefault() {
        return SystemMessageFactory.defaultConfig();
    }

    public static SystemMessageConfig withContext(
            SystemMessageConfig base,
            RetrievedContext retrievedContext,
            List<RecentMessage> recentMessage,
            List<LongTermMemoryData> longTermMemory

    ) {
        return new SystemMessageConfig(
                base.systemBehavior(),
                base.conversationRules(),
                base.decisionPolicy(),
                base.technicalResponsePolicy(),
                base.memoryPolicy(),
                base.antiRepetitionPolicy(),
                base.sensitiveTopicPolicy(),
                new RuntimeContext(
                        base.runtimeContext().Date(),
                        base.runtimeContext().userProfile(),
                        base.runtimeContext().memory(),
                        retrievedContext,
                        longTermMemory,
                        recentMessage,
                        base.runtimeContext().groupMemory(),
                        base.runtimeContext().responseContract()
                )
        );
    }

    public static SystemMessageConfig withMessageMemory(
            SystemMessageConfig base,
            RuntimeContext runtimeContext
    ) {
        return new SystemMessageConfig(
                base.systemBehavior(),
                base.conversationRules(),
                base.decisionPolicy(),
                base.technicalResponsePolicy(),
                base.memoryPolicy(),
                base.antiRepetitionPolicy(),
                base.sensitiveTopicPolicy(),
                new RuntimeContext(
                        runtimeContext.Date(),
                        runtimeContext.userProfile(),
                        runtimeContext.memory(),
                        runtimeContext.retrievedContext(),
                        runtimeContext.longTermMemory(),
                        runtimeContext.recentMessages(),
                        runtimeContext.groupMemory(),
                        base.runtimeContext().responseContract()
                )
        );
    }
}