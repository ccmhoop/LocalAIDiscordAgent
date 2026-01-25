package com.discord.LocalAIDiscordAgent.discord.listener;

import com.discord.LocalAIDiscordAgent.aiOllama.service.OllamaService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public abstract class MessageListener {

    private static final int MAX_INPUT_LENGTH = 4096;
    private static final int DISCORD_MAX_MESSAGE_LEN = 2000;

    public Mono<Void> processCommandAI(Message eventMessage, OllamaService ollamaService) {
        return Mono.just(eventMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .flatMap(message -> {
                    String content = message.getContent();

                    boolean mentioned =
                            content.toLowerCase().contains("@kier") ||
                                    content.contains("<@1379869980123992274>");
                    if (!mentioned) return Mono.empty();

                    content = content.replace("<@1379869980123992274>", "").trim();
                    if (content.isEmpty()) return Mono.empty();

                    if (content.length() > MAX_INPUT_LENGTH) {
                        return message.getChannel()
                                .flatMap(channel -> channel.createMessage(
                                        "I apologize, but your message is too long. Please limit your input to "
                                                + MAX_INPUT_LENGTH + " characters."))
                                .then();
                    }

                    String username = message.getAuthor().map(User::getUsername).orElse("unknown-user");
                    String guildId = message.getGuildId().map(Snowflake::asString).orElse("dm");
                    String channelId = message.getChannelId().asString();

                    String finalContent = content;

                    Mono<String> responseMono =
                            ollamaService.generateScottishResponseMono(finalContent, username, guildId, channelId);

                    return message.getChannel()
                            .flatMap(channel ->
                                    responseMono
                                            .flatMapMany(response -> {
                                                String cleanedResponse = cleanResponse(response);
                                                List<String> chunks = splitIntoChunks(cleanedResponse, DISCORD_MAX_MESSAGE_LEN);

                                                return Flux.fromIterable(chunks)
                                                        .concatMap(chunk -> channel.createMessage(chunk)
                                                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(120))
                                                                        .filter(MessageListener::isConnectionReset)
                                                                        .doBeforeRetry(rs -> log.warn(
                                                                                "Retrying after connection reset, attempt: {}",
                                                                                rs.totalRetries() + 1
                                                                        ))));
                                            })
                                            .then()
                            );
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

    private static String cleanResponse(String response) {
        if (response == null || response.isBlank()) {
            return "I'm sorry, I didn't understand you.";
        }

        return Arrays.stream(response.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    private static boolean isConnectionReset(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof SocketException se
                    && se.getMessage() != null
                    && se.getMessage().contains("Connection reset")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static List<String> splitIntoChunks(String s, int maxLen) {
        List<String> chunks = new ArrayList<>();
        if (s == null || s.isBlank()) {
            chunks.add("I'm sorry, I didn't understand you.");
            return chunks;
        }

        for (int i = 0; i < s.length(); i += maxLen) {
            chunks.add(s.substring(i, Math.min(i + maxLen, s.length())));
        }
        return chunks;
    }
}
