package com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.request;

import com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.records.FilterLLMContextRecord;
import org.springframework.util.Assert;

public class FilterRequest {

    private final String systemMessage;
    private final Class<? extends Record> outputRecordClass;

    private FilterLLMContextRecord context;

    public FilterRequest(Class<? extends Record> outputRecordClass, String systemMessage, FilterLLMContextRecord contextRecord) {
        Assert.notNull(outputRecordClass, "contextRecord cannot be null");
        Assert.notNull(systemMessage, "systemMessage cannot be null");
        this.outputRecordClass = outputRecordClass;
        this.systemMessage = systemMessage;
        this.context = contextRecord;
    }

    public String getSystemMessage() {
        return this.systemMessage;
    }

    public Class<? extends Record> getOutputRecordClass() {
        return this.outputRecordClass;
    }

    public FilterLLMContextRecord getContext() {
        if (this.context == null) {
            return null;
        }
        return this.context;
    }

}
