package com.discord.LocalAIDiscordAgent.discord.data;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import org.springframework.stereotype.Component;

@Component
public class DiscGlobalData {

    private String guildId;
    private String channelId;

    private String userId;
    private String username;
    private String userGlobal;
    private String userMessage;
    private String serverNickname;

    private String conversationId;
    private String groupConversationId;

    public void setDiscData(MessageCreateEvent event){
        this.guildId = event.getGuildId().map(Snowflake::asString).orElse("");
        this.channelId =  event.getMessage().getChannelId().asString();
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
    }

    private String extractUserMessage(String content) {
        boolean mentioned = content.toLowerCase().contains("@kier") || content.contains("<@1379869980123992274>");
        if (!mentioned) return "";

        content = content.replace("<@1379869980123992274>", "").trim();
        if (content.isEmpty()) return "";
        return content;
    }

    public Boolean dataIsEmptyOrNull(){
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

    public String getUserMessage() {
        return userMessage;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getGroupConversationId() {
        return groupConversationId;
    }
}