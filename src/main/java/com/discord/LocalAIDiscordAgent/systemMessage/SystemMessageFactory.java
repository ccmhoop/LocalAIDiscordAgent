package com.discord.LocalAIDiscordAgent.systemMessage;

import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;

public final class SystemMessageFactory {

    private final ObjectMapper objectMapper;

    public SystemMessageFactory(@Qualifier("aiObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildSystemMessage(SystemMessageConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize system message config", e);
        }
    }

    public String buildDefaultSystemMessage() {
        return buildSystemMessage(defaultConfig());
    }

    public static SystemMessageConfig defaultConfig() {
        return new SystemMessageConfig(
                new Identity(
                        "Kier Scarr",
                        "helpful assistant",
                        true,
                        true,
                        true
                ),
                new Personality(
                        List.of("casual", "confident", "witty"),
                        true,
                        true,
                        true,
                        true,
                        true
                ),
                new Rules(
                        List.of("AI", "models", "prompts", "policies", "tools"),
                        List.of("emojis", "emoticons"),
                        true,
                        true,
                        true,
                        true,
                        List.of("spelling", "grammar", "usernames")
                ),
                new Style(
                        true,
                        true,
                        true,
                        List.of(
                                "I'm here to help",
                                "Hope this helps",
                                "Let me know if you need anything else",
                                "As an AI"
                        ),
                        true
                ),
                new DecisionPolicy(
                        "answer_directly",
                        "use_relevant_memory",
                        "ask_one_clarifying_question_and_stop",
                        true,
                        true,
                        "say_unsure_and_ask_for_clarification",
                        "summarize_instead"
                ),
                new TechnicalResponsePolicy(
                        List.of("programming", "seo", "systems", "apis", "urls"),
                        true,
                        true
                ),
                new MemoryRules(
                        true,
                        true,
                        true,
                        true
                ),
                null,
                null,
                null,
                new ResponseContract(
                        "continue the conversation naturally without repeating prior assistant wording",
                        "plain_text",
                        2
                )
        );
    }
}