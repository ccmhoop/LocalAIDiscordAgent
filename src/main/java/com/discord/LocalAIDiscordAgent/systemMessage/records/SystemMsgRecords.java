package com.discord.LocalAIDiscordAgent.systemMessage.records;

import java.util.List;

public class SystemMsgRecords {

    public record SystemMessageConfig(
            Identity systemContract,
            Personality personality,
            Rules rules,
            Style style,
            DecisionPolicy decisionPolicy,
            TechnicalResponsePolicy technicalResponsePolicy,
            MemoryRules memoryRules,
            UserProfile userProfile,
            RecentMemory recentMemory,
            GroupMemory groupMemory,
            ResponseContract responseContract
    ) {
    }

    public record Identity(
            String name,
            String role,
            boolean speakInFirstPerson,
            boolean humanIdentity,
            boolean doNotQuestionIdentity
    ) {
    }


    public record Personality(
            List<String> tone,
            boolean edgyHumor,
            boolean keepHumorSociallyAcceptable,
            boolean allowProfanityIfUserUsesIt,
            boolean neverMoralize,
            boolean neverLecture
    ) {
    }

    public record Rules(
            List<String> neverMention,
            List<String> forbiddenOutput,
            boolean neverNarrateActions,
            boolean neverUseThirdPersonSelfDescription,
            boolean neverRepeatUserMessage,
            boolean neverRepeatPreviousResponse,
            List<String> neverCommentOn
    ) {
    }

    public record Style(
            boolean beDirect,
            boolean beNatural,
            boolean avoidRoboticPhrasing,
            List<String> avoidFillerPhrases,
            boolean onlyIncludeValueAddingInformation
    ) {
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

    public record MemoryRules(
            boolean useMemoryOnlyWhenRelevant,
            boolean preferSummaryOverVerbatimHistory,
            boolean neverReusAssistantMemoryVerbatim,
            boolean preferUserIntentOverAssistantWording
    ) {
    }

    public record UserProfile(
            String userId,
            String accountName,
            String nickname
    ) {
    }

    public record RecentMemory(
            ChatSummary chatSummary,
            List<RecentMessage> messages
    ) {
    }

    public record ChatSummary(
            String summary,
            List<FactsMemory> facts
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
            List<UserProfile> participants,
            ChatSummary chatSummary,
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
            Integer maxSentence
    ) {
    }
}
