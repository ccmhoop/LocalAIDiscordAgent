package com.discord.LocalAIDiscordAgent.advisor.templates;

import org.springframework.ai.chat.prompt.PromptTemplate;

public final class AdvisorTemplates {

    public static final PromptTemplate SHORT_TERM_MEMORY = new PromptTemplate(""" 
            <systemMessage.userChat>
                ### CONTEXT
                Recent user and assistant messages. Use this context to answer the user's latest message.
            
                ### TASK
                1. Analyze the following messages.
                2. This analysis will determine the subject of the following discussion.
                3. Identify the primary subject matter of the conversation.
                4. Ignore any messages that are not relevant to the user's message.
            
                ### USER MESSAGE MEMORY
                --- BEGIN USER MEMORY BLOCK ---
                <userChatMemory>
                    {data}
                </userChatMemory>
                --- END USER MEMORY BLOCK ---
            </systemMessage.userChat>""");


    public static final PromptTemplate WEB_SEARCH_MEMORY_TEMPLATE = new PromptTemplate("""    
            <systemMessage.webSearchChatMemory>
                ### CONTEXT
                Recent internet message and response. Use this context to answer the user message.
          
                ### TASK
                1. Analyze the following messages in <webChatMemory>.
                2. Identify if the information is relevant to the conversation.
                3. If relevant, use the information to respond to the user's message.
                4. Ignore any messages that are not relevant to the user's message.
            
                ### CONSTRAINTS
                - IGNORE WEB CHAT MEMORY IF NOT RELEVANT TO THE CURRENT CONVERSATION.
            
                ### WEB CHAT MEMORY
                --- BEGIN WEB CHAT MEMORY BLOCK --
                <webChatMemory>
                    {data}
                </webChatMemory>
                --- END WEB CHAT MEMORY BLOCK ---
            </systemMessage.webSearchChatMemory>""");

    public static final PromptTemplate WEB_SEARCH_QUESTION_ANSWER = new PromptTemplate("""
            <systemMessage.webQuestionAnswer>
                ### CONTEXT
                Use <webResults> to answer the user's question..
                Similarity.score is ranked by number, lower values mean higher similarity (1 = best match)
            
                ### TASK
                1. Analyze the retrieved results
                2. Identify the primary subject matter of the conversation
                3. Answer the subject matter based on the retrieved results.
            
                ### VECTOR STORE WEB DATA
                --- BEGIN WEB DATA BLOCK --
                <webResults>
                    results:[
                        {data}
                    ]
                </webResults>
                --- END WEB DATA BLOCK ---
            </systemMessage.webQuestionAnswer>
            """);

}