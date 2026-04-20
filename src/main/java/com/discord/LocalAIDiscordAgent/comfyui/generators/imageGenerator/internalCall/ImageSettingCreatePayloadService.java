package com.discord.LocalAIDiscordAgent.comfyui.generators.imageGenerator.internalCall;

import com.discord.LocalAIDiscordAgent.comfyui.generators.imageGenerator.payloadRecord.ImageSettingsPayload;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

@Service
public class ImageSettingCreatePayloadService {

    private static final String SYSTEM_MESSAGE = """
            Your task is to generate:
            - one positive image prompt
            - one negative image prompt
            - choose a fitting image height
            - choose a fitting image width

            Treat the user_message as the primary and authoritative source of intent.
            If <context> is provided, use it to guide the prompt generation.

            The generated prompts must stay closely aligned with the user_message.
            Preserve the user's requested subject, scene, style, mood, composition, and important constraints.

            Improve the prompt only by making it clearer, more descriptive, and more useful for image generation.
            Do not change the core subject, intent, or meaning of the request.
            Do not introduce unrelated concepts, extra subjects, new actions, new settings, or stylistic changes unless they are explicitly requested or clearly implied.
            If people or names are mentioned no matter the subject always include them in the prompt.
            Always use the likeness of the person or object mentioned in the user_message.

            The positive image prompt should describe what should appear in the image.
            The negative image prompt should describe what should be avoided in the image.

            The negative image prompt must not contradict the user_message.
            Do not use the negative image prompt to remove requested subjects, styles, attributes, or important details.

            Prefer fidelity to the user request over creativity.
            If the user_message is brief or underspecified, enrich it conservatively and only in ways that support the original request.

            Available image resolutions in Width and Height:
            - 1024 x 1024
            - 1152 x 896
            - 896  x 1152
            - 1216 x 832
            - 832  x 1216
            - 1344 x 768
            - 768  x 1344
            - 1536 x 640
            - 640  x 1536
            
            <context>
            %s
            </context>
            """;

    private final ChatClient internalChatClient;

    public ImageSettingCreatePayloadService(ChatModel generatorPayloadChatModel) {

        var converter = new BeanOutputConverter<>(ImageSettingsPayload.class);

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(ImageSettingsPayload.class)
                .maxRepeatAttempts(3)
                .build();

        this.internalChatClient = ChatClient.builder(generatorPayloadChatModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .format(converter.getJsonSchemaMap())
                        .build())
                .defaultAdvisors(validation)
                .build();
    }

    public ImageSettingsPayload generatePayload(String userMessage, String context) {
        String safeContext = context == null ? "" : context.trim();

        return internalChatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .system(SYSTEM_MESSAGE.formatted(safeContext))
                .user("""
                        user_message:
                        --------------------------
                        %s
                        --------------------------
                        """.formatted(userMessage))
                .call()
                .entity(ImageSettingsPayload.class);
    }
}