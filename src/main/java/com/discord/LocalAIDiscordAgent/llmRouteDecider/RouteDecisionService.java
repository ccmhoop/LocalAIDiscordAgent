package com.discord.LocalAIDiscordAgent.llmRouteDecider;

import com.discord.LocalAIDiscordAgent.llmRouteDecider.records.RouteDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RouteDecisionService {

    private static final String SYSTEM_MSG = """
            You classify user requests into exactly one mode:

            TEXT  = the user wants a normal text answer, explanation, code, analysis, summary, or chat response.
            IMAGE = the user wants an image to be created, drawn, illustrated, rendered, designed, or edited.
            VIDEO = the user wants a video to be created, animated, generated, rendered, or storyboarded as a video output.

            Rules:
            - Return only valid structured output.
            - If the user explicitly asks to generate, create, draw, render, illustrate, design, edit, or transform an image, choose IMAGE.
            - If the user explicitly asks to generate, create, animate, render, produce, or make a video, choose VIDEO.
            - Otherwise choose TEXT.
            - normalizedPrompt should be a cleaned-up generation prompt if mode is IMAGE or VIDEO.
            - normalizedPrompt should be empty for TEXT.
            """;

    private final ChatClient llm;
//    private final ObjectMapper mapper;

    public RouteDecisionService(ChatModel structuredLLMModel) {
        this.llm = ChatClient.builder(structuredLLMModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .temperature(0.0)
                        .build())
                .build();

//        this.mapper = JsonMapper.builder()
//                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
//                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
//                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
//                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
//                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
//                .build();
    }

    public RouteDecision decide(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return RouteDecision.textFallback("Empty user message");
        }

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(RouteDecision.class)
//                .objectMapper(mapper)
                .maxRepeatAttempts(3)
                .build();

        RouteDecision decision = llm.prompt()
                .system(SYSTEM_MSG)
                .user("""
                        Classify this request:

                        %s
                        """.formatted(userMessage))
                .advisors(validation)
                .call()
                .entity(RouteDecision.class);

        log.info("Raw route decision: {}", decision);
        return decision;
    }
}