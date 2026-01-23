package com.discord.LocalAIDiscordAgent.aiTools.websearch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AIWebFilterTool {

    @Tool(
            description = """
                    Filter webpage content that was retrieved using the webSearch tool.
                    The 'pageText' parameter MUST be the exact output returned by webSearch.
                    This tool must be called AFTER webSearch when answering questions about webpage details.
                    Focus only on the current question, not on previous conversation context.
                    """
    )
    public String webFilterText(
            @ToolParam(description = "Text previously retrieved from webSearch") String pageText,
            @ToolParam(description = "User question or information asked about") String question
    ) {
        if (pageText == null || pageText.isBlank()) {
            return "No page content available to filter.";
        }

        return """
                You are filtering webpage content.

                Task:
                - Identify and return ONLY the parts of the provided text that directly help answer the current question.
                - Ignore navigation, language lists, metadata, and unrelated sections.
                - Focus exclusively on the current question, disregarding any previous conversation context.
                - If no relevant content exists, say so explicitly.
                - Do not try to relate the content to previous conversations or questions.

                Current Question:
                """ + question + """

                Webpage text:
                """ + pageText;
    }
}
