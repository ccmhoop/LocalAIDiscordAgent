package com.discord.LocalAIDiscordAgent.systemMessage;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.*;

import java.util.List;

public final class SystemMessagePresets {

    private SystemMessagePresets() {}

    public static SystemMessageConfig qwenFriendlyDefault() {
        return SystemMessageFactory.defaultConfig();
    }

    public static SystemMessageConfig withContext(
            SystemMessageConfig base,
            RetrievedContext retrievedContext,
            List<RecentMessage> recentMessage

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
                runtimeContext
        );
    }
}