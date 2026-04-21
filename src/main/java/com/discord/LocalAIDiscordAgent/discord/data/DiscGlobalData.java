package com.discord.LocalAIDiscordAgent.discord.data;

import com.discord.LocalAIDiscordAgent.memory.chatMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import com.discord.LocalAIDiscordAgent.memory.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.UserProfile;
import lombok.Getter;
import org.springframework.ai.chat.messages.MessageType;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

@Getter
public class DiscGlobalData {

    private final String userId;
    private final String guildId;
    private final String username;
    private final String channelId;
    private final String userGlobal;
    private final String userMessage;
    private final String serverNickname;
    private final UserProfile userProfile;
    private final String conversationId;
    private final String groupConversationId;

    private GroupMemory groupChatMemory;
    private RecentMessage lastAssistantMsg;
    private List<RecentMessage> recentMessages = List.of();
    private List<LongTermMemoryData> longTermMemoryData = List.of();
    private List<RecentChatMemory> userMessages = List.of();
    private List<RecentChatMemory> assistantMessages = List.of();

    public DiscGlobalData(
            String userId,
            String guildId,
            String username,
            String channelId,
            String userGlobal,
            String userMessage,
            String serverNickname,
            String conversationId,
            String groupConversationId
    ) {
        this.userId = safe(userId);
        this.guildId = safe(guildId);
        this.username = safe(username);
        this.channelId = safe(channelId);
        this.userGlobal = safe(userGlobal);
        this.userMessage = safe(userMessage);
        this.serverNickname = safe(serverNickname);
        this.conversationId = safe(conversationId);
        this.groupConversationId = safe(groupConversationId);
        this.userProfile = new UserProfile(this.userId, this.username, this.serverNickname);
    }

    public void setDiscDataMemory(
            GroupMemory groupChatMemory,
            List<RecentMessage> recentMessages,
            List<LongTermMemoryData> longTermMemoryData,
            Map<MessageType, List<RecentChatMemory>> userAssistantMessages
    ) {
        this.groupChatMemory = groupChatMemory;
        this.recentMessages = recentMessages != null ? recentMessages : List.of();
        this.longTermMemoryData = longTermMemoryData != null ? longTermMemoryData : List.of();
        this.userMessages = userAssistantMessages != null
                ? userAssistantMessages.getOrDefault(USER, List.of())
                : List.of();
        this.assistantMessages = userAssistantMessages != null
                ? userAssistantMessages.getOrDefault(ASSISTANT, List.of())
                : List.of();

        setLastAssistantMessage();
    }

    public boolean isValid() {
        return !isBlank(userId)
                && !isBlank(channelId)
                && !isBlank(username)
                && !isBlank(conversationId)
                && !isBlank(groupConversationId);
    }

    public boolean hasEmptyPrompt() {
        return userMessage.isBlank();
    }

    private void setLastAssistantMessage() {
        this.lastAssistantMsg = null;

        if (recentMessages == null || recentMessages.isEmpty()) {
            return;
        }

        RecentMessage recentMessage = recentMessages.get(recentMessages.size() - 1);

        if (recentMessage != null
                && recentMessage.role() != null
                && recentMessage.role().toLowerCase().contains("assistant")) {
            this.lastAssistantMsg = new RecentMessage(
                    recentMessage.timestamp(),
                    recentMessage.role(),
                    recentMessage.content()
            );
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}