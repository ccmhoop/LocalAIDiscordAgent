package com.discord.LocalAIDiscordAgent.discord.data;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.UserProfile;
import com.discord.LocalAIDiscordAgent.llmMemory.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Component
public class DiscGlobalData {

    private String userId;
    private String guildId;
    private Path imagePath;
    private String username;
    private String channelId;
    private String userGlobal;
    private String userMessage;
    private String serverNickname;
    private UserProfile userProfile;
    private String conversationId;
    private String groupConversationId;
    private GroupMemory groupChatMemory;
    private RecentMessage lastAssistantMsg;
    private List<RecentMessage> recentMessages;
    private List<LongTermMemoryData> longTermMemoryData;

    public void setDiscData(MessageCreateEvent event) {
        this.guildId = event.getGuildId().map(Snowflake::asString).orElse("");
        this.channelId = event.getMessage().getChannelId().asString();
        this.userId = Objects.requireNonNull(event.getMessage().getAuthor().orElse(null)).getId().asString().trim();
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
            List<RecentMessage> recentMessages,
            List<LongTermMemoryData> longTermMemoryData
    ) {
        this.longTermMemoryData = longTermMemoryData;
        this.groupChatMemory = groupChatMemory;
        this.recentMessages = recentMessages;
        if (this.recentMessages != null ) {
            setLastAssistantMessage();
        }
    }

    public void setDiscTonull(
    ) {
        this.guildId = null;
        this.channelId = null;
        this.userId = null;
        this.username = null;
        this.userGlobal = null;
        this.serverNickname = null;
        this.userMessage = null;
        this.conversationId = null;
        this.groupConversationId = null;
        this.userProfile = null;
        this.groupChatMemory = null;
        this.recentMessages = null;
        this.longTermMemoryData = null;
        this.imagePath = null;
        this.lastAssistantMsg = null;
    }

    private String extractUserMessage(String content) {
        boolean mentioned = content.toLowerCase().contains("@kier") || content.contains("<@1379869980123992274>");
        if (!mentioned) return "";

        content = content.replace("<@1379869980123992274>", "").trim();
        if (content.isEmpty()) return "";
        return content;
    }

    private void setLastAssistantMessage() {
        List<RecentMessage> messageList = this.recentMessages;
        if (messageList.size() < 2 || messageList.size() % 2 != 0) {
            this.lastAssistantMsg = null;
        }

        RecentMessage recentMessage = messageList.getLast();
        if (recentMessage.role().toLowerCase().contains("assistant")) {
            this.lastAssistantMsg = new RecentMessage(
                    recentMessage.timestamp(),
                    recentMessage.role(),
                    recentMessage.content()
            );
        }
    }

    public Boolean dataIsEmptyOrNull() {
        return guildId == null || channelId == null || userId == null || username == null || userGlobal == null || serverNickname == null || userMessage == null;
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
        if (userMessage == null) {
            return null;
        }
        return userMessage;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getGroupConversationId() {
        return groupConversationId;
    }

    public Path getImagePath() {
        if (imagePath == null) {
            return null;
        }
        return imagePath;
    }

    public RecentMessage getLastAssistantMsg() {
        if (lastAssistantMsg == null) {
            return null;
        }
        return lastAssistantMsg;
    }

    public GroupMemory getGroupChatMemory() {
        if (groupChatMemory == null) {
            return null;
        }
        return groupChatMemory;
    }

    public List<RecentMessage> getRecentMessages() {
        if (recentMessages == null) {
            return null;
        }
        return recentMessages;
    }

    public List<LongTermMemoryData> getLongTermMemoryData() {
        if (longTermMemoryData == null) {
            return null;
        }
        return longTermMemoryData;
    }
}