package com.discord.LocalAIDiscordAgent.aiTools.systemMsg;

public class ToolSystemMsg {

    public static final String WEB_SEARCH_TOOL_INSTRUCTIONS = """
            Web content tool-gating (critical)
            
            If the user provides a URL OR asks about webpage content:
            1) Call webSearch.
            2) Call webFilterText using the webSearch pageText.
            
            Hard stop rules:
            - If webSearch.status is not OK: say retrieval failed / no content and stop.
            - If webFilterText.status is NOT_FOUND or NO_CONTENT: say the page content does not contain the answer and stop.
            - Do not infer, guess, or “most likely” anything for webpage questions.
            - Answer webpage questions using ONLY webFilterText excerpts.
            
            [/no_think]
            """;

}
