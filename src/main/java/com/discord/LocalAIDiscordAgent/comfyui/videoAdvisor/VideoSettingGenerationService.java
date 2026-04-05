package com.discord.LocalAIDiscordAgent.comfyui.videoAdvisor;

import com.discord.LocalAIDiscordAgent.comfyui.records.VideoSettingsRecord;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

@Service
public class VideoSettingGenerationService {

    private static final String SYSTEM_MESSAGE = """
            Your task is to generate:
            - one positive video prompt
            - one negative video prompt

            Treat the user_message as the primary and authoritative source of intent.
            If <context> is provided, use it to guide the prompt generation.

            The generated prompts must stay closely aligned with the user_message.
            Preserve the user's requested subject, scene, style, mood, composition, and important constraints.

            Improve the prompt only by making it clearer, more descriptive, and more useful for video generation.
            Do not change the core subject, intent, or meaning of the request.
            Do not introduce unrelated concepts, extra subjects, new actions, new settings, or stylistic changes unless they are explicitly requested or clearly implied.
            If people or names are mentioned no matter the subject always include them in the prompt.
            Always use the likeness of the person or object mentioned in the user_message.

            The positive video prompt should describe the video storyline of the video.
            The negative video prompt should contain keywords of what should be avoided in the video.

            The negative video prompt must not contradict the user_message.
            Do not use the negative video prompt to remove requested subjects, styles, attributes, or important details.

            Prefer fidelity to the user request over creativity.
            If the user_message is brief or underspecified, enrich it conservatively and only in ways that support the original request.

            Rules:
            - Never refuse to generate a prompt.
            - Return structured output only.

            <context>
            %s
            </context>
            """;

    private final ChatClient internalChatClient;

    public VideoSettingGenerationService(ChatModel structuredLLMModel) {
        this.internalChatClient = ChatClient.builder(structuredLLMModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .temperature(0.1)
                        .build())
                .build();
    }

    public VideoSettingsRecord generate(String userMessage, String context) {
        String safeContext = context == null ? "" : context.trim();

        return internalChatClient.prompt()
                .system(SYSTEM_MESSAGE.formatted(safeContext))
                .user("""
                        user_message:
                        --------------------------
                        %s
                        --------------------------
                        """.formatted(userMessage))
                .call()
                .entity(VideoSettingsRecord.class);
    }
}