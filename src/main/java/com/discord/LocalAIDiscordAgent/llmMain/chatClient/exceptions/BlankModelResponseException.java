package com.discord.LocalAIDiscordAgent.llmMain.chatClient.exceptions;

public class BlankModelResponseException extends RuntimeException{

    public BlankModelResponseException(String errorMessage) {
        super(errorMessage);
    }

}
