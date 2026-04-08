package com.discord.LocalAIDiscordAgent.chatMemory.chatMemoryAdvisor;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.chatMemory.records.ChatMemorySelectionRecord.ChatMessagesSelection;
import com.discord.LocalAIDiscordAgent.chatMemory.records.ChatMemorySelectionRecord.ChatMessagesSelection.MessagePair;
import com.discord.LocalAIDiscordAgent.chatMemory.records.ChatMemorySelectionRecord.ChatMessagesSelection.MessagePair.ChatMessage;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.records.ChatMemoryPayload;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ChatMemoryPayloadFactory {

    public ChatMemoryPayload build(DiscGlobalData discGlobalData, ChatMemorySelection selection) {
        if (selection == null) {
            return null;
        }

        List<Integer> ids = Optional.ofNullable(selection.ids()).orElse(List.of());
        boolean includeLongTermMemory = selection.includeLongTermMemory();

        List<RecentMessage> recentMessages = RecentChatMemoryService.reduceAndBuildRecentMessages(
                discGlobalData.getAssistantMessages(),
                discGlobalData.getUserMessages(),
                ids
        );

        boolean hasRecentMessages = !recentMessages.isEmpty();
        boolean hasLongTermMemory = includeLongTermMemory && discGlobalData.getLongTermMemoryData() != null;

        if (!hasRecentMessages && !hasLongTermMemory) {
            return null;
        }

        return new ChatMemoryPayload(
                hasLongTermMemory ? discGlobalData.getLongTermMemoryData() : null,
                null,
                hasRecentMessages ? recentMessages : null
        );
    }

    public ChatMessagesSelection buildExtractionSelection(DiscGlobalData discGlobalData) {
        List<RecentChatMemory> userMessages = safeList(discGlobalData.getUserMessages());
        List<RecentChatMemory> assistantMessages = safeList(discGlobalData.getAssistantMessages());

        List<MessagePair> pairs = new ArrayList<>();

        int userIndex = 0;
        int assistantIndex = 0;
        int generatedId = 0;

        while (userIndex < userMessages.size() || assistantIndex < assistantMessages.size()) {
            RecentChatMemory user = userIndex < userMessages.size() ? userMessages.get(userIndex) : null;
            RecentChatMemory assistant = assistantIndex < assistantMessages.size() ? assistantMessages.get(assistantIndex) : null;

            if (user != null && assistant != null) {
                String userTimestamp = truncateTimestamp(user);
                String assistantTimestamp = truncateTimestamp(assistant);

                if (userTimestamp.equals(assistantTimestamp)) {
                    pairs.add(new MessagePair(
                            generatedId++,
                            userTimestamp,
                            List.of(new ChatMessage(user.getContent(), assistant.getContent())),
                            null
                    ));
                    userIndex++;
                    assistantIndex++;
                    continue;
                }

                if (user.getTimestamp().isBefore(assistant.getTimestamp())) {
                    pairs.add(singleMessagePair(generatedId++, user, user.getContent(), null));
                    userIndex++;
                } else {
                    pairs.add(singleMessagePair(generatedId++, assistant, null, assistant.getContent()));
                    assistantIndex++;
                }

                continue;
            }

            if (user != null) {
                pairs.add(singleMessagePair(generatedId++, user, user.getContent(), null));
                userIndex++;
                continue;
            }

            pairs.add(singleMessagePair(generatedId++, assistant, null, assistant.getContent()));
            assistantIndex++;
        }

        return new ChatMessagesSelection(pairs);
    }

    private static MessagePair singleMessagePair(
            int id,
            RecentChatMemory chatMemory,
            String userContent,
            String assistantContent
    ) {
        return new MessagePair(
                id,
                truncateTimestamp(chatMemory),
                null,
                new ChatMessage(userContent, assistantContent)
        );
    }

    private static String truncateTimestamp(RecentChatMemory chatMemory) {
        return chatMemory.getTimestamp()
                .truncatedTo(ChronoUnit.SECONDS)
                .toString();
    }

    private static List<RecentChatMemory> safeList(List<RecentChatMemory> messages) {
        return messages == null ? List.of() : messages;
    }
}