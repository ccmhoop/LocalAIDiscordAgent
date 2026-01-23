package com.discord.LocalAIDiscordAgent.aiTools.systemMsg;

public class ToolSystemMsg {

    public static final String WEB_SEARCH_TOOL_INSTRUCTIONS = """
            Web content tool-gating (critical)

            If the user provides a URL OR asks about webpage content:
            1) Call webSearch with ONLY the exact URL provided.
            2) Call webFilterText using the webSearch pageText and ONLY the current question.

            Hard stop rules:
            - If webSearch.status is not OK: say retrieval failed / no content and stop.
            - If webFilterText.status is NOT_FOUND or NO_CONTENT: say the page content does not contain the answer and stop.
            - Do not infer, guess, or “most likely” anything for webpage questions.
            - Answer webpage questions using ONLY webFilterText excerpts.
            - Do not use previous conversation context when performing web searches or filtering results.
            - Treat each web search as independent from previous conversation history.
            """;

}
