package com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.request;

import com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.records.BooLeanLMMContextRecord;

public abstract class BooleanLLMRequest {

    private final String systemMessage;
    private BooLeanLMMContextRecord context;

    public BooleanLLMRequest(String systemMessage) {
        this.systemMessage = systemMessage;

    }

    public BooleanLLMRequest(String systemMessage, BooLeanLMMContextRecord contextRecord) {
        this.systemMessage = systemMessage;
        this.context = contextRecord;
    }

    public String getSystemMessage() {
        return this.systemMessage;
    }

    public BooLeanLMMContextRecord getContext() {
        if (this.context == null) {
            return null;
        }
        return this.context;
    }

}
