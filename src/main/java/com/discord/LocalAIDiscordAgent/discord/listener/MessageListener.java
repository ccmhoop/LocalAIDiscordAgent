package com.discord.LocalAIDiscordAgent.discord.listener;

import com.discord.LocalAIDiscordAgent.aiOllama.service.OllamaService;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Component
public abstract class MessageListener {

    public Mono<Void> processCommandAI(Message eventMessage, OllamaService ollamaService) {
        return Mono.just(eventMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .flatMap(message -> {
                    String content = message.getContent();

                    // Check if content contains !todo"
                    if (content.toLowerCase().contains("@kier") || content.contains("@1379869980123992274")) {

                        content = message.getContent().replace("<@1379869980123992274>", "").trim();

                        if (content.isEmpty()) {
                            return Mono.empty();
                        }

                        try {

                            String username = message.getAuthor().get().getUsername();
                            String response = ollamaService.generateKierResponse(content, username, message.getGuildId().get().asString(), message.getChannelId().asString() );

                            return message.getChannel()
                                    .flatMap(channel -> {

                                        // Normalize response: remove blank lines
                                        String cleanedResponse = Arrays.stream(response.split("\\R"))
                                                .map(String::trim)
                                                .filter(line -> !line.isEmpty())
                                                .collect(Collectors.joining("\n"));

                                        // Discord has a 2000 character limit for messages
                                        if (cleanedResponse.length() <= 2000) {
                                            return channel.createMessage(cleanedResponse);
                                        } else {
                                            Mono<Void> result = Mono.empty();

                                            for (int i = 0; i < cleanedResponse.length(); i += 2000) {
                                                int end = Math.min(i + 2000, cleanedResponse.length());
                                                String chunk = cleanedResponse.substring(i, end);
                                                result = result.then(channel.createMessage(chunk).then());
                                            }

                                            return result;
                                        }
                                    });
                        } catch (Exception e) {
                            log.error("Error generating or sending response: {}", e.getMessage());
                            return Mono.empty();
                        }

                    } else {
                        return Mono.empty();
                    }
                })
                .then();
    }

    public Mono<Void> processCommand(Message eventMessage) {
        return Mono.just(eventMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(message -> message.getContent().equalsIgnoreCase("!todo"))
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("Things to do today:\n - write a bot\n - eat lunch\n - play a game"))
                .then();
    }

}
