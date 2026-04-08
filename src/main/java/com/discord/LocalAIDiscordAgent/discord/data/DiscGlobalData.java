package com.discord.LocalAIDiscordAgent.discord.data;

import com.discord.LocalAIDiscordAgent.chatMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.UserProfile;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DiscGlobalData {

    private String userId = "";
    private String guildId = "";
    private Path imagePath;
    private String username = "";
    private String channelId = "";
    private String userGlobal = "";
    private String userMessage = "";
    private String serverNickname = "";
    private UserProfile userProfile;
    private String conversationId = "";
    private String groupConversationId = "";
    private GroupMemory groupChatMemory;
    private RecentMessage lastAssistantMsg;
    private List<RecentMessage> recentMessages = List.of();
    private List<LongTermMemoryData> longTermMemoryData = List.of();
    private List<RecentChatMemory> userMessages = List.of();
    private List<RecentChatMemory> assistantMessages = List.of();

    public boolean setDiscData(MessageCreateEvent event) {
        Message message = event.getMessage();
        User author = message.getAuthor().orElse(null);

        if (author == null) {
            return false;
        }

        Snowflake botId = event.getClient().getSelfId();

        this.guildId = event.getGuildId()
                .map(Snowflake::asString)
                .orElse("DM");

        this.channelId = message.getChannelId().asString();
        this.userId = author.getId().asString().trim();
        this.username = safe(author.getUsername());
        this.userGlobal = author.getGlobalName().orElse(this.username);

        this.serverNickname = message.getAuthorAsMember()
                .map(Member::getDisplayName)
                .onErrorReturn(this.username)
                .blockOptional()
                .orElse(this.username);

        this.userMessage = extractUserMessage(message.getContent(), botId);
        this.conversationId = this.guildId + ":" + this.channelId + ":" + this.userId;
        this.groupConversationId = this.guildId + ":" + this.channelId;
        this.userProfile = new UserProfile(this.userId, this.username, this.serverNickname);

        return true;
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
        return userMessage == null || userMessage.isBlank();
    }

    private String extractUserMessage(String content, Snowflake botId) {
        if (content == null || content.isBlank()) {
            return "";
        }

        return content
                .replaceAll("<@!?" + botId.asString() + ">", "")
                .trim();
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public void setImagePath(Path imagePath) {
        this.imagePath = imagePath;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getUserGlobal() {
        return userGlobal;
    }

    public String getServerNickname() {
        return serverNickname;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getGroupConversationId() {
        return groupConversationId;
    }

    public Path getImagePath() {
        return imagePath;
    }

    public RecentMessage getLastAssistantMsg() {
        return lastAssistantMsg;
    }

    public GroupMemory getGroupChatMemory() {
        return groupChatMemory;
    }

    public List<RecentMessage> getRecentMessages() {
        return recentMessages;
    }

    public List<RecentChatMemory> getUserMessages() {
        return userMessages;
    }

    public List<RecentChatMemory> getAssistantMessages() {
        return assistantMessages;
    }

    public List<LongTermMemoryData> getLongTermMemoryData() {
        return longTermMemoryData;
    }
}