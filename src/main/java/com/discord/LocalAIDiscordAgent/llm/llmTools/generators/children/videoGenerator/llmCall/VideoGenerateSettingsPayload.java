package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.llmCall;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.llmCall.SettingsPayloadGenerator;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.payload.VideoSettingsPayload;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class VideoGenerateSettingsPayload extends SettingsPayloadGenerator<VideoSettingsPayload> {

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
            
            <context>
            %s
            </context>
            """;

    private static final String USER_INSTRUCT = """
            Generate a video based on the user_message.
            
            user_message:
            --------------------------
            %s
            --------------------------
            """;

    public VideoGenerateSettingsPayload(ChatModel llmPayloadModel) {
        super(VideoSettingsPayload.class, llmPayloadModel);
    }

    @Override
    public String setSystemMessage() {
        return SYSTEM_MESSAGE;
    }

    @Override
    public String setUserInstruction() {
        return USER_INSTRUCT;
    }

}