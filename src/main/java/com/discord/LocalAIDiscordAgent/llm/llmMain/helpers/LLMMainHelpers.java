package com.discord.LocalAIDiscordAgent.llm.llmMain.helpers;

import com.discord.LocalAIDiscordAgent.llm.llmMain.exceptions.BlankModelResponseException;
import org.springframework.ai.chat.model.ChatResponse;

public final class LLMMainHelpers {

    public static String extractOutputTextAsString(ChatResponse chatResponse) {
        if (chatResponse == null
                || chatResponse.getResult().getOutput().getText() == null
                || chatResponse.getResult().getOutput().getText().isBlank()
        ) {
            throw new BlankModelResponseException("Model returned blank output");
        }
        return chatResponse.getResult().getOutput().getText().trim();
    }

}
