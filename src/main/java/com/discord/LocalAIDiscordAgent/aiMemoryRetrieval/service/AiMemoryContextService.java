package com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.service;

import com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.vectorMemories.backgroundMemory.BackgroundMemoryContextBuilder;
import com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.vectorMemories.personalityUserMemory.PersonalityMemoryContextBuilder;
import com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.vectorMemories.situationalMemory.SituationalMemoryContextBuilder;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiMemoryContextService {

    private final PersonalityMemoryContextBuilder userPersonalityContextBuilder;
    private final SituationalMemoryContextBuilder situationalContextBuilder;
    private final BackgroundMemoryContextBuilder backgroundContextBuilder;

    public AiMemoryContextService(
            PersonalityMemoryContextBuilder userPersonalityContextBuilder,
            SituationalMemoryContextBuilder situationalContextBuilder,
            BackgroundMemoryContextBuilder backgroundContextBuilder
    ) {
        this.userPersonalityContextBuilder = userPersonalityContextBuilder;
        this.situationalContextBuilder = situationalContextBuilder;
        this.backgroundContextBuilder = backgroundContextBuilder;
    }

    public void buildMessages( List<Message> messages, String userId, String userMessage) {

        userPersonalityContextBuilder.buildUserPersonalityMemories(userId, messages);

        backgroundContextBuilder.buildBackgroundMemories(userId, userMessage, messages);

        situationalContextBuilder.buildSituationalMemories(userId, userMessage, messages);

        messages.add(new UserMessage(userMessage));

        userPersonalityContextBuilder.processPersonality(userId, userMessage);

    }

}



