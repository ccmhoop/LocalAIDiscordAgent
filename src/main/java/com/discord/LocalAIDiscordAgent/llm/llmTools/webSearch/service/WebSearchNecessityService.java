package com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
public class WebSearchNecessityService {

    private static final String SYSTEM_MESSAGE = """
            You are a strict web-search necessity classifier.

            Your task is to decide whether a web search is genuinely necessary to answer the user_message,
            using the retrieved_context inside <context> only as supporting context.

            Rules:
            1. Treat user_message as the primary source of intent.
            2. Use <context> only to clarify the user_message, not as an automatic reason to search.
            3. Check these conditions before making a decision:
               - the user_message is phrased as a question
               - <context> contains factual information
               - the topic could theoretically be searched
            4. Return true only if the user_message clearly requires one or more of the following:
               - current or recent information
               - factual verification
               - external lookup
               - precise real-world data
               - source-backed claims
               - news, prices, laws, regulations, schedules, availability, releases, rankings, or other changeable facts
            5. Return true if the user explicitly asks to:
               - search
               - look something up
               - verify
               - check the latest information
               - find sources
               - find citations
               - google
               - web search
            6. Prefer false over true if the request can be answered without external lookup.

            Return only one word: true or false.

            <context>
            %s
            </context>
            """;

    private final ChatClient internalChatClient;

    public WebSearchNecessityService(ChatModel llmStructuredModel) {
        this.internalChatClient = ChatClient.builder(llmStructuredModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .temperature(0.0)
                        .build())
                .build();
    }

    public boolean needsWebSearch(String userMessage, String context) {
        String safeContext = context == null ? "" : context.trim();

        String response = internalChatClient.prompt()
                .system(SYSTEM_MESSAGE.formatted(safeContext))
                .user("""
                        user_message:
                        --------------------------
                        %s
                        --------------------------
                        Return only one word: true or false.
                        """.formatted(userMessage))
                .call()
                .content();

        return "true".equals(
                Optional.ofNullable(response)
                        .orElse("")
                        .trim()
                        .toLowerCase(Locale.ROOT)
        );
    }
}