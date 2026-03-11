package com.discord.LocalAIDiscordAgent.systemMessage;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.*;

import java.util.List;
import java.util.Map;

public final class SystemMessagePresets {

    private SystemMessagePresets() {
    }

    public static SystemMessageConfig qwenFriendlyDefault() {
        return SystemMessageFactory.defaultConfig();
    }

    public static SystemMessageConfig withNoProfanity(SystemMessageConfig base) {
        return new SystemMessageConfig(
                base.systemContract(),
                new Personality(
                        base.personality().tone(),
                        base.personality().edgyHumor(),
                        base.personality().keepHumorSociallyAcceptable(),
                        false,
                        base.personality().neverMoralize(),
                        base.personality().neverLecture()
                ),
                base.rules(),
                base.style(),
                base.decisionPolicy(),
                base.technicalResponsePolicy(),
                base.memoryRules(),
                base.userProfile(),
                base.recentMemory(),
                base.groupMemory(),
                base.responseContract()

        );
    }

    public static SystemMessageConfig withCustomIdentity(
            SystemMessageConfig base,
            String name,
            String role
    ) {
        return new SystemMessageConfig(
                new Identity(
                        name,
                        role,
                        base.systemContract().speakInFirstPerson(),
                        base.systemContract().humanIdentity(),
                        base.systemContract().doNotQuestionIdentity()
                ),
                base.personality(),
                base.rules(),
                base.style(),
                base.decisionPolicy(),
                base.technicalResponsePolicy(),
                base.memoryRules(),
                base.userProfile(),
                base.recentMemory(),
                base.groupMemory(),
                base.responseContract()
        );
    }

    public static SystemMessageConfig withDoNotQuestionIdentity(
            SystemMessageConfig base,
            boolean doNotQuestionIdentity
    ) {
        return new SystemMessageConfig(
                new Identity(
                        base.systemContract().name(),
                        base.systemContract().role(),
                        base.systemContract().speakInFirstPerson(),
                        base.systemContract().humanIdentity(),
                        doNotQuestionIdentity
                ),
                base.personality(),
                base.rules(),
                base.style(),
                base.decisionPolicy(),
                base.technicalResponsePolicy(),
                base.memoryRules(),
                base.userProfile(),
                base.recentMemory(),
                base.groupMemory(),
                base.responseContract()
        );
    }

    public static SystemMessageConfig withCustomTechnicalDomains(
            SystemMessageConfig base,
            List<String> domains
    ) {
        return new SystemMessageConfig(
                base.systemContract(),
                base.personality(),
                base.rules(),
                base.style(),
                base.decisionPolicy(),
                new TechnicalResponsePolicy(
                        domains,
                        base.technicalResponsePolicy().mayUseStructuredAnswers(),
                        base.technicalResponsePolicy().mayUseListsOrStepsWhenUseful()
                ),
                base.memoryRules(),
                base.userProfile(),
                base.recentMemory(),
                base.groupMemory(),
                base.responseContract()
        );
    }

    public static SystemMessageConfig withCustomMemoryTags(
            SystemMessageConfig base,
            Map<String, List<String>> formats
    ) {
        return new SystemMessageConfig(
                base.systemContract(),
                base.personality(),
                base.rules(),
                base.style(),
                base.decisionPolicy(),
                base.technicalResponsePolicy(),
                new MemoryRules(
                        base.memoryRules().useMemoryOnlyWhenRelevant(),
                        base.memoryRules().preferSummaryOverVerbatimHistory(),
                        base.memoryRules().neverReusAssistantMemoryVerbatim(),
                        base.memoryRules().preferUserIntentOverAssistantWording()
                ),
                base.userProfile(),
                base.recentMemory(),
                base.groupMemory(),
                base.responseContract()
        );
    }

    public static SystemMessageConfig withAdditionalForbiddenOutput(
            SystemMessageConfig base,
            List<String> additionalForbiddenOutput
    ) {
        List<String> mergedForbiddenOutput = new java.util.ArrayList<>(base.rules().forbiddenOutput());
        mergedForbiddenOutput.addAll(additionalForbiddenOutput);

        return new SystemMessageConfig(
                base.systemContract(),
                base.personality(),
                new Rules(
                        base.rules().neverMention(),
                        List.copyOf(mergedForbiddenOutput),
                        base.rules().neverNarrateActions(),
                        base.rules().neverUseThirdPersonSelfDescription(),
                        base.rules().neverRepeatUserMessage(),
                        base.rules().neverRepeatPreviousResponse(),
                        base.rules().neverCommentOn()
                ),
                base.style(),
                base.decisionPolicy(),
                base.technicalResponsePolicy(),
                base.memoryRules(),
                base.userProfile(),
                base.recentMemory(),
                base.groupMemory(),
                base.responseContract()
        );
    }

    public static SystemMessageConfig withCustomFillerPhrases(
            SystemMessageConfig base,
            List<String> fillerPhrases
    ) {
        return new SystemMessageConfig(
                base.systemContract(),
                base.personality(),
                base.rules(),
                new Style(
                        base.style().beDirect(),
                        base.style().beNatural(),
                        base.style().avoidRoboticPhrasing(),
                        fillerPhrases,
                        base.style().onlyIncludeValueAddingInformation()
                ),
                base.decisionPolicy(),
                base.technicalResponsePolicy(),
                base.memoryRules(),
                base.userProfile(),
                base.recentMemory(),
                base.groupMemory(),
                base.responseContract()
        );
    }


    public static SystemMessageConfig withMessageMemory(
            SystemMessageConfig base,
            UserProfile userProfile,
            RecentMemory recentMemory,
            GroupMemory groupMemory
    ) {
        return new SystemMessageConfig(
                base.systemContract(),
                base.personality(),
                base.rules(),
                base.style(),
                base.decisionPolicy(),
                base.technicalResponsePolicy(),
                base.memoryRules(),
                userProfile,
                recentMemory,
                groupMemory,
                base.responseContract()
        );
    }
}