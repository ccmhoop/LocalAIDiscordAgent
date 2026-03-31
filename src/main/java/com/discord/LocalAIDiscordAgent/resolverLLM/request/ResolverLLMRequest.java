package com.discord.LocalAIDiscordAgent.resolverLLM.request;

import com.discord.LocalAIDiscordAgent.resolverLLM.records.ResolverLMMContextRecord;

public abstract class ResolverLLMRequest {

    private final String systemMessage;
    private ResolverLMMContextRecord context;

    public ResolverLLMRequest(String systemMessage) {
        this.systemMessage = systemMessage;

    }

    public ResolverLLMRequest(String systemMessage, ResolverLMMContextRecord contextRecord) {
        this.systemMessage = systemMessage;
        this.context = contextRecord;
    }

    public String getSystemMessage() {
        return this.systemMessage;
    }

    public ResolverLMMContextRecord getContext() {
        if (this.context == null) {
            return null;
        }
        return this.context;
    }

}
