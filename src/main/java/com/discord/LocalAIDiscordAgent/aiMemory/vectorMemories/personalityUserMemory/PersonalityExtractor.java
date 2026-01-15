package com.discord.LocalAIDiscordAgent.aiMemory.vectorMemories.personalityUserMemory;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PersonalityExtractor {

    private static final List<String> STRONG_SIGNALS = List.of(
            "i prefer",
            "i don't want",
            "do not use",
            "avoid",
            "never",
            "always",
            "must",
            "stop using",
            "i like"
    );

    public Optional<PersonalityCandidate> extract(String messageText) {

        if (messageText == null || messageText.isBlank()) {
            return Optional.empty();
        }

        String lower = messageText.toLowerCase();

        boolean matchesSignal = STRONG_SIGNALS.stream()
                .anyMatch(lower::contains);

        if (!matchesSignal) {
            return Optional.empty();
        }

        // Reject transient emotional messages
        if (lower.contains("today") || lower.contains("right now")) {
            return Optional.empty();
        }

        return Optional.of(
                new PersonalityCandidate(
                        messageText,
                        inferSubject(lower),
                        inferAliases(lower)
                )
        );
    }

    private String inferSubject(String text) {

        if (text.contains("code") || text.contains("library") || text.contains("framework")) {
            return "engineering_preferences";
        }

        if (text.contains("explain") || text.contains("answer")) {
            return "communication_style";
        }

        if (text.contains("auth") || text.contains("token") || text.contains("security")) {
            return "architecture_constraints";
        }

        return "general_preference";
    }

    private List<String> inferAliases(String text) {

        List<String> aliases = new ArrayList<>();

        if (text.contains("local storage")) aliases.add("localStorage");
        if (text.contains("framework")) aliases.add("frameworks");
        if (text.contains("library")) aliases.add("libraries");
        if (text.contains("concise")) aliases.add("conciseness");

        return aliases;
    }

    public record PersonalityCandidate(
            String rawText,
            String subject,
            List<String> aliases
    ) {}

}
