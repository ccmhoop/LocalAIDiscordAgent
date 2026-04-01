package com.discord.LocalAIDiscordAgent.webSearch.llmRequests.resolver;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.request.BooleanLLMRequest;
import com.discord.LocalAIDiscordAgent.llmResolvers.booleanLLM.records.BooLeanLMMContextRecord;


public class IsWebSearchRequest extends BooleanLLMRequest {

    private static final String SYSTEM_MESSAGE = """
            You are a strict web-search necessity classifier.
            
            Your task is to decide whether a web search is genuinely necessary to answer the user_message, using the retrieved_context inside <context> only as supporting context.
            
            Rules:
            1. Treat user_message as the primary source of intent.
            2. Use <context> only to clarify the user_message, not as an automatic reason to search.
            3. Check these conditions before making a decision:
               - the user_message is phrased as a question
               - <context> contains factual information
               - the topic could theoretically be searched
            4. Analyze if the user_message clearly requires one or more of the following:
               - current or recent information
               - factual verification
               - external lookup
               - precise real-world data
               - source-backed claims
               - news, prices, laws, regulations, schedules, availability, releases, rankings, or other changeable facts
            5. Always accept when the use explicitly asks to:
               - search
               - look something up
               - verify
               - check the latest information
               - find sources or citations
            6. Always decline when the message can be answered well without web search, including:
               - banter
               - greetings
               - acknowledgements
               - filler
               - reactions
               - encouragement
               - casual conversation
               - rhetorical questions
               - creative writing
               - brainstorming
               - rewriting
               - summarization of provided content
               - translation
               - general advice not requiring current external facts
            
            <context>
            %s
            </context>
            """;

    public IsWebSearchRequest(DiscGlobalData discGlobalData) {
        super(SYSTEM_MESSAGE,
                new BooLeanLMMContextRecord(
                        discGlobalData.getGroupChatMemory(),
                        null,
                        discGlobalData.getRecentMessages(),
                        null,
                        null
                ));
    }

}
