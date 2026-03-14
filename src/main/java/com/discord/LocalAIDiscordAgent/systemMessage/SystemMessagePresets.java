package com.discord.LocalAIDiscordAgent.systemMessage;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.*;

public final class SystemMessagePresets {

    public static SystemMessageConfig qwenFriendlyDefault() {
        return SystemMessageFactory.defaultConfig();
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
                runtimeContext
        );
    }
}