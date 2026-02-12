package com.discord.LocalAIDiscordAgent.chatClient.helpers;

import com.discord.LocalAIDiscordAgent.chatClient.exceptions.BlankModelResponseException;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.Map;

public final class ChatClientHelpers {

    private static final String USERNAME = "username";
    private static final String GUILD_ID = "guildId";
    private static final String CHANNEL_ID = "channelId";

    public static String extractOutputTextAsString(ChatResponse chatResponse) {
        if (chatResponse == null
                || chatResponse.getResult().getOutput().getText() == null
                || chatResponse.getResult().getOutput().getText().isBlank()
        ) {
            throw new BlankModelResponseException("Model returned blank output");
        }
        return chatResponse.getResult().getOutput().getText().trim();
    }

    public static String buildMetaDataConversationId(Map<String, String> metadata) {
        String safeGuild = (metadata.get(GUILD_ID).isEmpty()) ? "dm" : metadata.get(GUILD_ID);
        String safeChannel = (metadata.get(CHANNEL_ID).isEmpty()) ? "dm" : metadata.get(CHANNEL_ID);
        String safeUser = (metadata.get(USERNAME).isEmpty()) ? "unknown-user" : metadata.get(USERNAME);
        return safeGuild + ":" + safeChannel + ":" + safeUser;
    }
}
