package com.discord.LocalAIDiscordAgent.toolSystemMessage.records;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.UserProfile;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;

import java.util.List;
import java.util.Map;

public record ToolSystemMsgRecords(
        List<String> executionOrder,
        List<AvailableTools> availableTools,
        ToolUsePolicy toolUsePolicy,
        AssistantDecision assistantDecision,
        ToolCall toolCall,
        FinalAnswer finalAnswer,
        ToolRuntimeContext runtimeContext
        ) {

    public record AvailableTools(
            String name,
            String description,
            Map<String, String> inputs) {
    }

    public record ToolUsePolicy(
            boolean contextFirst,
            boolean retrievedContextHasPriority,
            boolean skipToolCallWhenContextSufficient,
            boolean fedRetrievedContextBeforeToolDecision,
            boolean useToolOnlyAfterContextCheck,
            boolean neverCallToolBeforeContextIsEvaluated,
            String sufficiencyRule,
            String decisionsRules,
            List<String> useToolIf,
            List<String> skipToolIf,
            boolean neverFakeToolResults,
            boolean preferSingleBestTool,
            String ifNoToolNeeded,
            String ifToolNeeded
    ) {
    }

    public record AssistantDecision(
            boolean mustRunBeforeToolCall,
            List<String> inputSources,
            FieldsRecord fields
    ) {
        public record FieldsRecord(
                FieldTypes contextSufficient,
                FieldTypes reason,
                FieldTypes action
        ) {
        }
    }

    public record ToolCall(
            AllowedOnlyWhen allowedOnlyWhen,
            FieldsRecord fields
    ) {
        public record AllowedOnlyWhen(
                String assistantDecision_action,
                boolean assistantDecision_contextSufficient
        ) {
        }

        public record FieldsRecord(
                FieldTypes toolName,
                FieldTypes arguments
        ) {
        }
    }

    public record FinalAnswer(
            FieldsRecord fields
    ) {
        public record FieldsRecord(
                FieldTypes answer,
                FieldTypes sourcesBasis,
                FieldTypes usedTools
        ) {
        }
    }

    public record ToolRuntimeContext(
            String Date,
            UserProfile userProfile,
            List<MergedWebQAItem> retrievedContext,
            RecentMessage lastAssistantMsg
    ){
    }

    public record FieldTypes(
            String type,
            List<String> enums,
            boolean required
    ){
    }

}
