package com.discord.LocalAIDiscordAgent.llmRouteDecider;

import com.discord.LocalAIDiscordAgent.comfyui.imageGenerator.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.llmRouteDecider.records.RouteDecision;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class RouteDecisionService {

    private final ChatClient llm;

    private static final String SYSTEM_MSG = """
            You classify user requests into exactly one mode:
            
            TEXT  = the user wants a normal text answer, explanation, code, analysis, summary, or chat response.
            IMAGE = the user wants an image to be created, drawn, illustrated, rendered, designed, or edited.
            VIDEO = the user wants a video to be created, animated, generated, rendered, or storyboarded as a video output.
            MUSIC = the user wants music, a song, instrumental, beat, soundtrack, melody, audio track, or musical composition to be created or generated.
            
            Rules:
            - Return only valid structured output.
            - If the user explicitly asks to generate, create, draw, render, illustrate, design, edit, or transform an image, choose IMAGE.
            - If the user explicitly asks to generate, create, animate, render, produce, or make a video, choose VIDEO.
            - If the user explicitly asks to generate, create, compose, produce, make, or write music, a song, beat, instrumental, soundtrack, melody, or lyrics for music generation, choose MUSIC.
            - Otherwise choose TEXT.
            - normalizedPrompt should be a cleaned-up generation prompt if mode is IMAGE, VIDEO, or MUSIC.
            - normalizedPrompt should be empty for TEXT.
            """;

    public RouteDecisionService(ChatModel structuredLLMModel) {
        var converter = new BeanOutputConverter<>(RouteDecision.class);

        Map<String, Object> schemaFormat = converter.getJsonSchemaMap();

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(RouteDecision.class)
                .objectMapper(MapperUtils.lenientJsonMapper())
                .maxRepeatAttempts(3)
                .build();

        this.llm = ChatClient.builder(structuredLLMModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .temperature(0.0)
                        .format(schemaFormat)
                        .build())
                .defaultAdvisors(validation)
                .build();
    }

    public RouteDecision decide(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return RouteDecision.textFallback("Empty user message");
        }

        RouteDecision decision = llm.prompt()
                .system(SYSTEM_MSG)
                .user("""
                        Classify this request:
                        
                        %s
                        """.formatted(userMessage))
                .call()
                .entity(RouteDecision.class);

        log.info("Raw route decision: {}", decision);
        return decision;
    }
}