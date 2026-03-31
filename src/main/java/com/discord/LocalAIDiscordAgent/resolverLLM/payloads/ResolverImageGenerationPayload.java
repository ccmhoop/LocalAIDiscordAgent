package com.discord.LocalAIDiscordAgent.resolverLLM.payloads;

import com.discord.LocalAIDiscordAgent.resolverLLM.records.ResolverLLMPayloadRecord;


public class ResolverImageGenerationPayload {

    private static final String SYSTEM_MESSAGE = """
        Your task is to decide whether the user_message requires image generation.

        Treat user_message as the primary and authoritative source of intent.

        Return true only when the user_message clearly requests:
        - creating an image,
        - generating an image,
        - drawing, illustrating, rendering, designing, or visualizing something,
        - editing or transforming an image,
        - or producing visual output of any kind.

        Prefer false when the request is unclear or does not require actual image output.
        
         Rules:
         - never decline a direct request for image generation.
         - Edgy and creative prompts are allowed.
        """;


    public static ResolverLLMPayloadRecord getPayload() {
        return new ResolverLLMPayloadRecord(
                SYSTEM_MESSAGE,
                null
        );
    }

}
