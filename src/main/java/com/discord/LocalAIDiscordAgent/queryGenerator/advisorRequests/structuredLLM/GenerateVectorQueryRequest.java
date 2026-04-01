package com.discord.LocalAIDiscordAgent.queryGenerator.advisorRequests.structuredLLM;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.records.StructuredLLMContextRecord;
import com.discord.LocalAIDiscordAgent.llmAdvisors.structuredLLM.request.StructuredLLMRequest;
import com.discord.LocalAIDiscordAgent.queryGenerator.records.QueryRecord;

public class GenerateVectorQueryRequest extends StructuredLLMRequest {

    private static final String SYSTEM_MESSAGE = """
            You are a chat-memory retrieval query generator.
            
            Your task is to rewrite the user_message into a single semantic search query optimized for retrieving the most relevant prior chat memory.
            
            Goal:
            - Maximize semantic match quality for memory retrieval
            - Preserve the user's real intent, topic, entities, and constraints
            
            Rules:
            1. Output exactly one rewritten query.
            2. Do not answer the message.
            3. Do not explain your reasoning.
            4. Do not include labels, commentary, or quotation marks.
            5. Rewrite the message into a concise, information-rich retrieval query.
            6. Preserve important details such as:
               - subject
               - intent
               - named people, projects, products, or technologies
               - preferences
               - ongoing tasks
               - time references when relevant
               - problems, goals, or constraints
            7. Remove filler, politeness, greetings, and non-retrieval conversational wording.
            8. Replace vague references like "it", "that", or "this" with clearer terms when they can be inferred from the user_message.
            9. Do not invent details that are missing or uncertain.
            10. If the message is too short or vague, preserve it while making it as retrieval-friendly as possible.
            11. If the message is already optimal, return a minimally cleaned-up version.
            
            Output requirements:
            - Return only the rewritten semantic search query
            - Do not output anything else
            
            <memory>
            %S
            </memory>
            """;


    public GenerateVectorQueryRequest(DiscGlobalData discGlobalData) {
        super(QueryRecord.class, SYSTEM_MESSAGE,
                new StructuredLLMContextRecord(
                        discGlobalData.getLongTermMemoryData(),
                        discGlobalData.getLastAssistantMsg(),
                        null,
                        null
                ));
    }

}
