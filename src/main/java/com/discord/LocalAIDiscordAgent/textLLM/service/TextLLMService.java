package com.discord.LocalAIDiscordAgent.textLLM.service;

import com.discord.LocalAIDiscordAgent.textLLM.calls.TextLLMCalls;
import com.discord.LocalAIDiscordAgent.textLLM.llm.TextLLM.TextLLMImageGenerationSettings;
import org.springframework.stereotype.Service;

@Service
public class TextLLMService {

    private final TextLLMCalls llmCalls;

    public TextLLMService(TextLLMCalls llmCalls) {
        this.llmCalls = llmCalls;
    }

    public String generateQuery(){
        return llmCalls.generateVectorQuery();
    }

    public TextLLMImageGenerationSettings generateImageSettings(){
        return llmCalls.generateImageGenerationSettings();
    }

}
