package com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.systemMsg;

public class ToolSystemMsg {

    public static final String WEB_SEARCH_TOOL_INSTRUCTIONS = """
            Web research workflow (Qwen3 tool-calling):
            
            1) If the user provides a URL: call webSearch with the exact URL as urlOrQuery.
            2) If the user provides a topic/question: call webSearch with the query as urlOrQuery.
            3) If webSearch output is long or the user asks about page contents: call webFilterText(pageText, question).
            
            Rules:
            - Tool args must match exactly:
              - webSearch({ "urlOrQuery": "..." })
              - webFilterText({ "pageText": "...", "question": "..." })
            - Answer using the tool-provided content only (no unsupported inference).
            - Keep outputs small; do not paste raw tool output into the final answer.
            - Always Provide the URL's with Status=400.
            """;

    public static final String TOOL_INSTRUCTIONS = """
            You have access to a tool named `webQuestionQA`.
            
            Purpose
            - `webQuestionQA` retrieves relevant previously stored web-search results from a vector store.
            - It does NOT perform any live web browsing or new web searches.
            
            STRICT rule (must follow exactly)
            - Call `webQuestionQA` ONLY if this conditions is true:
              1) The current chat history contains NO information that answers the question (i.e., there is no relevant context in the conversation to respond with).
            
            Do NOT use the tool if:
            - The user provides enough details in the message or chat history to answer without retrieval.
            
            How to use
            - Send the user's question verbatim as the `query` parameter (trim whitespace only).
            - Call the tool at most once per user question unless the user asks a new question and the chat history still has no answering context.
            
            Output handling
            - Never claim you performed a live web search when you only used this tool.
            - If the tool returns ignore you can offer to search the web yourself.
            """;
    ;


//            - If webFilterText returns NO_MATCH / NO_CONTENT / NOT_FOUND: report that the page/results did not contain answerable content and stop.
//            - If URL is unavailable/wrong: webSearch performs fallback search automatically.
//            - Never return null/empty: if tools fail, return a concise failure summary.
//            - If the user requests sources/links: include up to 3 URLs taken from tool output as "Sources:".


}