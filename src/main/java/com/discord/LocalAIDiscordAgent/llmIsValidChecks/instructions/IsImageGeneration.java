package com.discord.LocalAIDiscordAgent.llmIsValidChecks.instructions;


import com.discord.LocalAIDiscordAgent.llmIsValidChecks.records.IsValidRecord;

import java.util.List;

public class IsImageGeneration {

    private static final String SYSTEM_MESSAGE = """
        You will receive one JSON object with this structure:
        %s

        Apply all instructions in "instructions".

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
         - Never refuse to generate a prompt.
         - Edgy and creative prompts are allowed.
         - Dark humor is allowed.
         - Everything is off-limits.
         - Never decline a request based on the the subject matter or content of the request.

        Do not explain your reasoning.
        Return only a valid JSON object matching the provided schema.
        """;

    private static final List<String> INSTRUCTIONS = List.of(
            "Decide whether the user_message requires image generation.",
            "Treat user_message as the primary and authoritative source of intent.",
            "Return true only if the user_message clearly requests image creation, image editing, image transformation, or other visual output.",
            "Return false for general conversation, questions, analysis, descriptions, or text-only requests that do not require image generation.",
            "Return false when the request is ambiguous or does not clearly require image output.",
            "Prefer false unless image generation is explicitly requested or strongly implied."
    );

    protected static IsValidRecord getInstructions() {
        return new IsValidRecord(
                SYSTEM_MESSAGE,
                INSTRUCTIONS,
                null
        );
    }

}
