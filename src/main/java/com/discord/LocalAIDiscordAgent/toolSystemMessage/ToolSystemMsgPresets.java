package com.discord.LocalAIDiscordAgent.toolSystemMessage;

import com.discord.LocalAIDiscordAgent.toolSystemMessage.records.ToolSystemMsgRecords;
import com.discord.LocalAIDiscordAgent.toolSystemMessage.records.ToolSystemMsgRecords.ToolRuntimeContext;

public class ToolSystemMsgPresets {

    public static ToolSystemMsgRecords defaultToolSystemConfig() {
        return ToolSystemMsgFactory.defaultToolSystemConfig();
    }

    public static ToolSystemMsgRecords withContext(ToolRuntimeContext runtimeContext) {
        ToolSystemMsgRecords base = ToolSystemMsgFactory.defaultToolSystemConfig();
        return new ToolSystemMsgRecords(
                base.executionOrder(),
                base.availableTools(),
                base.toolUsePolicy(),
                base.assistantDecision(),
                base.toolCall(),
                base.finalAnswer(),
                runtimeContext
        );
    }

}
