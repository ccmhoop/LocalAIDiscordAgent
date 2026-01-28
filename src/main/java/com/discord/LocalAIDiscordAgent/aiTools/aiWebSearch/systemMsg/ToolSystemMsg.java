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


//            - If webFilterText returns NO_MATCH / NO_CONTENT / NOT_FOUND: report that the page/results did not contain answerable content and stop.
//            - If URL is unavailable/wrong: webSearch performs fallback search automatically.
//            - Never return null/empty: if tools fail, return a concise failure summary.
//            - If the user requests sources/links: include up to 3 URLs taken from tool output as "Sources:".


}