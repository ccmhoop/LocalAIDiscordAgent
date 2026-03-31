package com.discord.LocalAIDiscordAgent.textLLM.calls;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.textLLM.llm.TextLLM;
import com.discord.LocalAIDiscordAgent.textLLM.llm.TextLLM.TextLLMImageGenerationSettings;
import com.discord.LocalAIDiscordAgent.textLLM.llm.TextLLM.TextLLMQueryGenerationRecord;
import com.discord.LocalAIDiscordAgent.textLLM.payload.TextLLMImagePromptPayload;
import com.discord.LocalAIDiscordAgent.textLLM.payload.TextLLMVectorQueryPayload;
import com.discord.LocalAIDiscordAgent.textLLM.records.TextLLMPayloadRecord;
import org.springframework.stereotype.Component;

@Component
public class TextLLMCalls {

    private final TextLLM llm;
    private final DiscGlobalData discGlobalData;

    public TextLLMCalls(
            TextLLM textLLM,
            DiscGlobalData discGlobalData
    ) {
        this.discGlobalData = discGlobalData;
        this.llm = textLLM;
    }

    public String generateVectorQuery() {
        TextLLMPayloadRecord payload = TextLLMVectorQueryPayload.getPayload(discGlobalData);
        Record record = llm.call(payload);
        if (record instanceof TextLLMQueryGenerationRecord(String query)) {
            return query;
        }
        return null;
    }

    public TextLLMImageGenerationSettings generateImageGenerationSettings() {
        TextLLMPayloadRecord payload = TextLLMImagePromptPayload.getPayload();
        Record record = llm.call(payload);
        if (record instanceof TextLLMImageGenerationSettings recordOutPut) {
            return recordOutPut;
        }
        return null;
    }

}
