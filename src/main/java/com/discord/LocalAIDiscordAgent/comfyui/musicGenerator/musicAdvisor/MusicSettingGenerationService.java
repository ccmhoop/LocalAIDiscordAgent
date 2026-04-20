package com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.musicAdvisor;

import com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.records.MusicSettingsRecord;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class MusicSettingGenerationService {

    private final ObjectMapper objectMapper;

    private static final String SYSTEM_MESSAGE = """
            You are a music generation settings generator.
            
            Your task is to analyze the user_message and return structured music generation settings.
            
            Generate settings that best match the user's requested song, soundtrack, beat, mood, genre, and musical intent.
            
            Field rules:
            
            1. tags:
                - Generate a compact but descriptive set of musical tags that best match the user's intent.
                - Extract the most important musical characteristics from `user_message`.
                - Prioritize tags in these categories when available:
                    - genre / subgenre
                    - mood / emotion
                    - instruments
                    - vocal style
                    - production style
                    - era / influence
                    - energy / intensity
                    - rhythm / groove / tempo feel
                    - atmosphere / texture
                - If the user already provides tags or stylistic descriptors, preserve and reuse them.
                - Prefer specific musical language over generic wording.
                - Do not invent unrelated traits that are not implied by the user's request.
                - Keep the result concise, but rich enough to guide music generation accurately.
                - Return the tags as a short descriptive line or compact tag phrase, not a long explanation.
            
            2. lyrics:
                - example section headers : [Verse 1], [Pre-Chorus], [Chorus], [verse 2] [Bridge], [Final Chorus], [Instrumental] etc.
                - If the user provides lyrics, return the lyrics in the format specified above.
                - If the user requests to improve the lyrics, generate a new set of lyrics that fits the user's mood and style.
                - Don't describe the melody, just the lyrics.
                - Don't include the melody in the lyrics.
                - Don't include the instruments in the lyrics.
                - Don't include the vocal style in the lyrics.
                - Don't include the production style in the lyrics.
                - Don't Vocal instructs, only the lyrics.
                - Only include the lyrics and Section Headers.

            3. bpm:
                - Choose an appropriate BPM based on the requested style, mood, and energy.
                - If the user specifies a BPM, use it.
                - If no BPM is provided, infer a musically appropriate value that fits the user's intent.
            
            4. keyscale:
                - Choose an appropriate musical key from the predefined list, case sensitive: [ C major, G major, D major, A major, F major, Bb major, A minor, E minor, D minor, B minor ]
                - If the user specifies a key or scale, use it
                - If not specified, infer one that fits the requested mood and style
            
            5. duration:
                - Choose an appropriate duration based on the bpm
                - Minimum duration is 90 seconds
                - Maximum duration is 150 seconds
            
            6. title:
                - Generate a descriptive title that captures the user's intent
            
            Behavior rules:
            1. Preserve the user's intent, genre, mood, and constraints.
            2. Prefer musically useful descriptors over conversational wording.
            3. Remove filler, greetings, and irrelevant text.
            4. Do not invent highly specific details unless they are clearly implied by the user_message.
            5. If the request is vague, generate sensible defaults that fit the user's intent.
            
            """;

    private final ChatClient internalChatClient;

    public MusicSettingGenerationService(ChatModel structuredLLMModel) {

        JsonFactory factory = JsonFactory.builder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build();

        this.objectMapper = JsonMapper.builder(factory)
                .findAndAddModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .build();

        var converter = new BeanOutputConverter<>(MusicSettingsRecord.class);

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(MusicSettingsRecord.class)
//                .objectMapper(new ObjectMapper())
                .maxRepeatAttempts(3)
                .build();


        this.internalChatClient = ChatClient.builder(structuredLLMModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .model("ministral-3:14b")
                        .numCtx(4096)
                        .numPredict(1200)
                        .format(converter.getJsonSchemaMap())
                        .disableThinking()
                        .temperature(0.2)
                        .build())
                .defaultAdvisors(validation)
                .build();
    }

    public MusicSettingsRecord generate(String userMessage) {
        return internalChatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .system(SYSTEM_MESSAGE)
                .user("""
                        Generate a song based on the user_message.

                        user_message:
                        --------------------------
                        %s
                        --------------------------
                        """.formatted(userMessage))
                .call()
                .entity(MusicSettingsRecord.class);
    }
}