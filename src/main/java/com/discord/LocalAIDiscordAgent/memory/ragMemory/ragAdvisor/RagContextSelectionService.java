package com.discord.LocalAIDiscordAgent.memory.ragMemory.ragAdvisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
public class RagContextSelectionService {

    private static final String QUERY_SYSTEM_MESSAGE = """
            You are a helpful assistant specialized in creating short detailed semantic search queries.
            - keep the query concise and accurate to the user's intent.

            Rules:
            1. Return only the query text.
            2. Do not return JSON.
            3. Do not return explanations.
            4. Keep the query compact but semantically rich.
            5. Preserve the main subject, intent, entities, and constraints from the user message.
            """;

    private static final String RELEVANCE_SYSTEM_MESSAGE = """
            You are a strict relevance classifier.

            Decide whether the retrieved context is usable for answering the user_message.

            Return true only if the retrieved context directly answers the user_message
            or would materially improve the answer.

            Return false if the context is:
            - unrelated
            - only loosely related
            - too generic
            - ambiguous
            - missing the important details needed to help answer

            Prefer false over true.

            Return only one word: true or false.
            """;

    private final ChatClient internalChatClient;

    public RagContextSelectionService(ChatModel llmStructuredModel) {
        this.internalChatClient = ChatClient.builder(llmStructuredModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .temperature(0.0)
                        .build())
                .build();
    }

    public String buildQuery(String userMessage) {
        String query = internalChatClient.prompt()
                .system(QUERY_SYSTEM_MESSAGE)
                .user("""
                        Convert the user message below into a detailed compact semantic search query.
                        - keep the query concise and accurate to the user message.

                        User message:
                        --------------------------
                        %s
                        --------------------------
                        """.formatted(userMessage))
                .call()
                .content();

        return normalize(query);
    }

    public boolean isRelevant(String retrievedContext, String userMessage) {
        String relevanceText = internalChatClient.prompt()
                .system(RELEVANCE_SYSTEM_MESSAGE)
                .user("""
                        Retrieved context:
                        --------------------------
                        %s
                        --------------------------

                        User message:
                        --------------------------
                        %s
                        --------------------------
                        """.formatted(retrievedContext, userMessage))
                .call()
                .content();

        return "true".equals(
                Optional.ofNullable(relevanceText)
                        .orElse("")
                        .trim()
                        .toLowerCase(Locale.ROOT)
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}