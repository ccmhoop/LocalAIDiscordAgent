package com.discord.LocalAIDiscordAgent.chatClient.exceptions;

public class BlankModelResponseException extends RuntimeException{

    public BlankModelResponseException(String errorMessage) {
        super(errorMessage);
    }
}
