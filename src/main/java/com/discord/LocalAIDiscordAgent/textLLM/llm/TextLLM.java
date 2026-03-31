package com.discord.LocalAIDiscordAgent.textLLM.llm;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.textLLM.records.TextLLMContextRecord;
import com.discord.LocalAIDiscordAgent.textLLM.records.TextLLMPayloadRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class TextLLM {

    private final DiscGlobalData discGlobalData;
    private final ChatClient llm;

    public TextLLM(
            DiscGlobalData discGlobalData,
            ChatClient textLLMClient
    ) {
        this.discGlobalData = discGlobalData;
        this.llm = textLLMClient;
    }

    public Record call(@NonNull TextLLMPayloadRecord payload) {
        Class<? extends Record> outputType =
                payload.context() == null
                        ? TextLLMImageGenerationSettings.class
                        : TextLLMQueryGenerationRecord.class;

        TextLLMInstructions instructions = new TextLLMInstructions(
                payload.instructions(),
                payload.context(),
                discGlobalData.getUserMessage()
        );

        return callLLM(
                payload.systemMsg(),
                instructions,
                outputType
        );
    }

    private <T extends Record> T callLLM(
            String systemTemplate,
            TextLLMInstructions payload,
            Class<T> outputType
    ) {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .build();

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(outputType)
                .objectMapper(mapper)
                .maxRepeatAttempts(3)
                .build();

        return llm.prompt()
                .system(systemTemplate)
                .user(payload.userMessage())
                .advisors(validation)
                .call()
                .entity(outputType);
    }

    public record TextLLMInstructions(
            List<String> instructions,
            TextLLMContextRecord retrievedContext,
            String userMessage
    ) {}

    @JsonDeserialize
    public record TextLLMQueryGenerationRecord(
            @JsonProperty(required = true, value = "query")
            String query
    ) {}

    @JsonDeserialize(using = TextLLMImagePromptDeserializer.class)
    public record TextLLMImageGenerationSettings(
            String positivePrompt,
            String negativePrompt,
            int pixelWidth,
            int pixelHeight
    ) {}

    public static class TextLLMImagePromptDeserializer extends JsonDeserializer<TextLLMImageGenerationSettings> {
        @Override
        public TextLLMImageGenerationSettings deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            String positivePrompt = node.path("positivePrompt").asText();
            String negativePrompt = node.path("negativePrompt").asText();
            int pixelWidth = node.path("pixelWidth").asInt();
            int pixelHeight = node.path("pixelHeight").asInt();

            return new TextLLMImageGenerationSettings(
                    positivePrompt,
                    negativePrompt,
                    pixelWidth,
                    pixelHeight
            );
        }
    }

}
