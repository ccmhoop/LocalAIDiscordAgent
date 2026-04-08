package com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.musicAdvisor;

import com.discord.LocalAIDiscordAgent.comfyui.imageGenerator.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.records.MusicSettingsRecord;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

@Service
public class MusicSettingGenerationService {

    private static final String SYSTEM_MESSAGE = """
            You are a music generation settings generator.
            
            Your task is to analyze the user_message and return structured music generation settings.
            
            Generate settings that best match the user's requested song, soundtrack, beat, mood, genre, and musical intent.
            
            Field rules:
            
            1. tags:
               - generate a list of musical tags that best describe the user's intent
               - Include the most important musical characteristics from the user_message
               - Prefer tags and descriptors such as genre, mood, instruments, vocal style, production style, era, energy, rhythm, and atmosphere
               - Keep it compact but descriptive
               - If the user provides tags, use them
            
            2. lyrics:
               - example header : [Verse 1], [Pre-Chorus], [Chorus], [verse 2] [Bridge], [Final Chorus], [Instrumental] etc.
               - If Generate Lyrics based on user_message is true.
               - If the user provides lyrics, return the lyrics in the format specified above.
               - If the user requests to improve the lyrics, generate a new set of lyrics that fits the user's mood and style.

            3. bpm:
               - Choose an appropriate BPM based on the requested style, mood, and energy
               - If the user specifies a BPM, use it
               - If no BPM is provided, infer a musically appropriate value

            4. keyscale:
               - Choose an appropriate musical key from the predefined list: [ C Major, G Major, D Major, A Major, F Major, Bb Major, A Minor, E Minor, D Minor, B Minor ]
               - If the user specifies a key or scale, use it
               - If not specified, infer one that fits the requested mood and style
            
            5. title:
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
        this.internalChatClient = ChatClient.builder(structuredLLMModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .temperature(0.1)
                        .build())
                .build();
    }

    public MusicSettingsRecord generate(String userMessage) {

        return internalChatClient.prompt()
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