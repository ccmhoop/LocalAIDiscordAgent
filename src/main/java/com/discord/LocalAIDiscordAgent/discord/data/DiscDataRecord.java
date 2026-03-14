package com.discord.LocalAIDiscordAgent.discord.data;

public record DiscDataRecord(
        String guildId,
        String channelId,
         String userId,
         String username,
         String userGlobal,
         String serverNickname,
         String userMessage) {
}
