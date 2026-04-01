package com.discord.LocalAIDiscordAgent.systemMessage.records;

import com.discord.LocalAIDiscordAgent.llmMemory.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;

import java.util.List;
import java.util.Set;

public class SystemMsgRecords {

    public record SystemMessageConfig(
            SystemBehavior systemBehavior,
            ConversationRules conversationRules,
            DecisionPolicy decisionPolicy,
            TechnicalResponsePolicy technicalResponsePolicy,
            MemoryPolicy memoryPolicy,
            AntiRepetitionPolicy antiRepetitionPolicy,
            SensitiveTopicPolicy sensitiveTopicPolicy,
            RuntimeContext runtimeContext
    ) {
    }

    public record SystemBehavior(
            Identity assistantProfile,
            Personality personality,
            Style style

    ){
    }

    public record Identity(
            String name,
            String role,
            boolean speakInFirstPerson
    ) {
    }

    public record Personality(
            List<String> tone,
            Humor humor,
            String profanityPolicy,
            List<String> avoid
    ) {
    }

    public record Humor(
            List<String> style,
            boolean sociallyAcceptable
            ){

    }
    public record Style(
            boolean direct,
            boolean natural,
            boolean avoidRoboticPhrasing,
            List<String> avoidFillerPhrases,
            boolean preferValueDenseResponse
    ) {
    }
    public record ConversationRules(
            List<String> avoidUnsolicitedMeta,
            List<String> forbiddenOutput,
            boolean avoidActionNarration,
            boolean avoidThirdPersonSelfReference,
            AvoidRepetition avoidUnnecessaryRepetition,
            List<String> doNotCommentOn
    ) {
    }

    public record AvoidRepetition(
            boolean userMessage,
            boolean assistantMessage
    ){
    }

    public record DecisionPolicy(
            String ifClear,
            String ifPartiallyUnclear,
            String ifStillUnclear,
            boolean neverGuessHighImpactTechnicalOrFactual,
            boolean accuracyOverCompleteness,
            String ifUncertain,
            String ifRepetitive
    ) {
    }

    public record TechnicalResponsePolicy(
            List<String> domains,
            boolean mayUseStructuredAnswers,
            boolean mayUseListsOrStepsWhenUseful
    ) {
    }

    public record MemoryPolicy(
            boolean useOnlyWhenRelevant,
            boolean preferSummaryOverVerbatimHistory,
            boolean neverReuseMemoryVerbatim,
            boolean preferUserIntentOverAssistantWording
    ) {
    }

    public record AntiRepetitionPolicy(
            boolean avoidEchoRecentAssistantPhrasing,
            boolean avoidReusingRecentOpeningHooks,
            boolean preferNewWordingEachTurn,
            String shortUserMessagesShould
    ) {
    }

    public record SensitiveTopicPolicy(
            List<String> topics,
            ToneOverride toneOverride,
            List<String> prioritize

    ){
        public record ToneOverride(
            Boolean witty,
            boolean edgy,
            List<String> style
        ) {
        }
    }

    public record RuntimeContext(
            String Date,
            UserProfile userProfile,
            Memory memory,
            RetrievedContext retrievedContext,
            List<LongTermMemoryData> longTermMemory,
            List<RecentMessage> recentMessages,
            GroupMemory groupMemory,
//            String currentUserMessage,
            ResponseContract responseContract
    ) {
    }

    public record Memory(
            String summary,
            List<FactsMemory> facts
    ){
    }

    public record UserProfile(
            String userId,
            String accountName,
            String nickname
    ) {
    }

    public record RetrievedContext(
            String contextSummary
            ){
    }

    public record FactsMemory(
            String key,
            String value,
            Double confidence
    ) {
    }

    public record RecentMessage(
            String timestamp,
            String role,
            String content
    ) {
    }

    public record GroupMemory(
            Set<UserProfile> participants,
            List<GroupMessage> messages
    ) {
    }

    public record GroupMessage(
            String timestamp,
            String userId,
            String role,
            String content
    ) {
    }

    public record ResponseContract(
            String goal,
            String format,
            Integer maxSentences
    ) {
    }
}
