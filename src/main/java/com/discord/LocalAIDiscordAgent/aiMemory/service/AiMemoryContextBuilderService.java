package com.discord.LocalAIDiscordAgent.aiMemory.service;

import com.discord.LocalAIDiscordAgent.aiMemory.vectorMemories.backgroundMemory.BackgroundMemoryContextBuilder;
import com.discord.LocalAIDiscordAgent.aiMemory.vectorMemories.personalityUserMemory.PersonalityMemoryContextBuilder;
import com.discord.LocalAIDiscordAgent.aiMemory.vectorMemories.situationalMemory.SituationalMemoryContextBuilder;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiMemoryContextBuilderService {

    private final PersonalityMemoryContextBuilder userPersonalityContextBuilder;
    private final SituationalMemoryContextBuilder situationalContextBuilder;
    private final BackgroundMemoryContextBuilder backgroundContextBuilder;

    public AiMemoryContextBuilderService(
            PersonalityMemoryContextBuilder userPersonalityContextBuilder,
            SituationalMemoryContextBuilder situationalContextBuilder,
            BackgroundMemoryContextBuilder backgroundContextBuilder
    ) {
        this.userPersonalityContextBuilder = userPersonalityContextBuilder;
        this.situationalContextBuilder = situationalContextBuilder;
        this.backgroundContextBuilder = backgroundContextBuilder;
    }

    public List<Message> buildMessages(String systemPersona, String userId, String userMessage) {

        List<Message> messages = new ArrayList<>();

        messages.add(new SystemMessage(systemPersona));

        userPersonalityContextBuilder.buildUserPersonalityMemories(userId, messages);

        backgroundContextBuilder.buildBackgroundMemories(userId, userMessage, messages);

        situationalContextBuilder.buildSituationalMemories(userId, userMessage, messages);

        messages.add(new UserMessage(userMessage));

        userPersonalityContextBuilder.processPersonality(userId, userMessage);

        return messages;
    }

}



