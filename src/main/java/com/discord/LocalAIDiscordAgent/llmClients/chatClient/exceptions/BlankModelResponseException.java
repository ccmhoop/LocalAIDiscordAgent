package com.discord.LocalAIDiscordAgent.llmClients.chatClient.exceptions;

public class BlankModelResponseException extends RuntimeException{

    public BlankModelResponseException(String errorMessage) {
        super(errorMessage);
    }

}
