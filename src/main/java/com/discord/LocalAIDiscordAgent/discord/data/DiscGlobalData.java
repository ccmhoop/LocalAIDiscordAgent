package com.discord.LocalAIDiscordAgent.discord.data;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.UserProfile;
import com.discord.LocalAIDiscordAgent.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiscGlobalData {

    private String userId;
    private String guildId;
    private String username;
    private String channelId;
    private String userGlobal;
    private String userMessage;
    private String serverNickname;
    private UserProfile userProfile;
    private String conversationId;
    private String groupConversationId;
    private GroupMemory groupChatMemory;
    private List<RecentMessage> recentMessages;
    private List<LongTermMemoryData> longTermMemoryData;

    public void setDiscData(MessageCreateEvent event) {
        this.guildId = event.getGuildId().map(Snowflake::asString).orElse("");
        this.channelId = event.getMessage().getChannelId().asString();
        this.userId = event.getMessage().getAuthor().get().getId().asString().trim();
        this.username = event.getMessage().getAuthor().map(User::getUsername).orElse("");
        this.userGlobal = event.getMessage().getAuthor().get().getGlobalName().orElse("");
        this.serverNickname = event.getMessage().getAuthorAsMember()
                .map(Member::getDisplayName)
                .blockOptional()
                .orElseGet(() -> event.getMessage().getAuthor().map(User::getUsername).orElse(""));
        this.userMessage = extractUserMessage(event.getMessage().getContent());
        this.conversationId = guildId + ":" + channelId + ":" + userId;
        this.groupConversationId = guildId + ":" + channelId;
        this.userProfile = new UserProfile(userId, username, serverNickname);
    }

    public void setDiscDataMemory(
            GroupMemory groupChatMemory,
            List<RecentMessage> recentMessage,
            List<LongTermMemoryData> longTermMemoryData
    ) {
        this.longTermMemoryData = longTermMemoryData;
        this.groupChatMemory = groupChatMemory;
        this.recentMessages = recentMessage;
    }


    private String extractUserMessage(String content) {
        boolean mentioned = content.toLowerCase().contains("@kier") || content.contains("<@1379869980123992274>");
        if (!mentioned) return "";

        content = content.replace("<@1379869980123992274>", "").trim();
        if (content.isEmpty()) return "";
        return content;
    }

    public Boolean dataIsEmptyOrNull() {
        return guildId == null || channelId == null || userId == null || username == null || userGlobal == null || serverNickname == null || userMessage == null;
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

    public GroupMemory getGroupChatMemory() {
        return groupChatMemory;
    }

    public List<RecentMessage> getRecentMessages() {
        return recentMessages;
    }

    public List<LongTermMemoryData> getLongTermMemoryData() {
        return longTermMemoryData;
    }
}