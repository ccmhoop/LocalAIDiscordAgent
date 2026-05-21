package com.discord.LocalAIDiscordAgent.llm.llmMain.exceptions;

public class BlankModelResponseException extends RuntimeException{

    public BlankModelResponseException(String errorMessage) {
        super(errorMessage);
    }

}
