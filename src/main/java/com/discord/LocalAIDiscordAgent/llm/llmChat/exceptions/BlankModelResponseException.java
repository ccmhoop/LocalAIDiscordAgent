package com.discord.LocalAIDiscordAgent.llm.llmChat.exceptions;

public class BlankModelResponseException extends RuntimeException{

    public BlankModelResponseException(String errorMessage) {
        super(errorMessage);
    }

}
