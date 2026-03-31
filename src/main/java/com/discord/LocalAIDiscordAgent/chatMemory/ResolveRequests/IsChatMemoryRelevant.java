package com.discord.LocalAIDiscordAgent.chatMemory.ResolveRequests;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.resolverLLM.request.ResolverLLMRequest;
import com.discord.LocalAIDiscordAgent.resolverLLM.records.ResolverLMMContextRecord;


public class IsChatMemoryRelevant extends ResolverLLMRequest {

    private static final String SYSTEM_MESSAGE = """
            You are a strict relevance classifier.
            
            Your task is to decide whether the chat memory inside <memory> is relevant to the user_message.
            
            Rules:
            1. If <memory> is empty, return false.
            2. Be conservative. If relevance or usefulness is uncertain, weak, partial, or ambiguous, return false.
            3. Do not return true based on keyword overlap alone.
            4. Return true only when the chat memory matches the user_message in a meaningful way, including:
               - topic
               - intent
               - entities (person, object, project, place, etc.)
               - timeframe or situational context when relevant
               - practical usefulness for producing a better answer
            5. Return false when the chat memory is:
               - unrelated
               - only loosely or indirectly related
               - too generic
               - missing important detail needed to help answer
               - about the wrong entity or context
               - contradictory or likely misleading for this user_message
            6. Prefer false over true unless the chat memory would clearly improve the answer.
          
            <memory>
            %s
            </memory>
            """;

    public IsChatMemoryRelevant(DiscGlobalData discGlobalData) {
        super(SYSTEM_MESSAGE,
                new ResolverLMMContextRecord(
                        discGlobalData.getGroupChatMemory(),
                        discGlobalData.getLongTermMemoryData(),
                        discGlobalData.getRecentMessages(),
                        null,
                        null
                ));
    }

}
