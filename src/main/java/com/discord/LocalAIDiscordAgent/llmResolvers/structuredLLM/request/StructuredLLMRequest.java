package com.discord.LocalAIDiscordAgent.llmResolvers.structuredLLM.request;

import com.discord.LocalAIDiscordAgent.llmResolvers.structuredLLM.records.StructuredLLMContextRecord;

public class StructuredLLMRequest {

    private final String systemMessage;
    private final Class<? extends Record> outputRecordClass;

    private StructuredLLMContextRecord context;

    public StructuredLLMRequest(Class<? extends Record> outputRecordClass, String systemMessage , StructuredLLMContextRecord context) {
        this.systemMessage = systemMessage;
        this.outputRecordClass = outputRecordClass;
        this.context = context;
    }

    public StructuredLLMRequest(Class<? extends Record> recordClass, String systemMessage ) {
        this.systemMessage = systemMessage;
        this.outputRecordClass = recordClass;
    }

    public String getSystemMessage() {
        return this.systemMessage;
    }

    public Class<? extends Record> getOutputRecordClass() {
        return this.outputRecordClass;
    }

    public StructuredLLMContextRecord getContext() {
        if (this.context == null) {
            return null;
        }
    return this.context;
    }

}
