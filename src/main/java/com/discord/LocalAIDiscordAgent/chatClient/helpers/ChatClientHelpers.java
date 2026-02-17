package com.discord.LocalAIDiscordAgent.chatClient.helpers;

import com.discord.LocalAIDiscordAgent.chatClient.exceptions.BlankModelResponseException;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.Map;

import static com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey.*;

public final class ChatClientHelpers {

    public static String extractOutputTextAsString(ChatResponse chatResponse) {
        if (chatResponse == null
                || chatResponse.getResult().getOutput().getText() == null
                || chatResponse.getResult().getOutput().getText().isBlank()
        ) {
            throw new BlankModelResponseException("Model returned blank output");
        }
        return chatResponse.getResult().getOutput().getText().trim();
    }

    public static String buildConversationId(Map<DiscDataKey, String> discDataMap) {
        String safeGuild = (discDataMap.get(GUILD_ID).isEmpty()) ? "dm" : discDataMap.get(GUILD_ID);
        String safeChannel = (discDataMap.get(CHANNEL_ID).isEmpty()) ? "dm" : discDataMap.get(CHANNEL_ID);
        String safeUser = (discDataMap.get(USERNAME).isEmpty()) ? "unknown-user" : discDataMap.get(USERNAME);
        return safeGuild + ":" + safeChannel + ":" + safeUser;
    }
}
