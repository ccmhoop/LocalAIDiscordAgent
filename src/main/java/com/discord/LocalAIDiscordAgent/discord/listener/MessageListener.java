package com.discord.LocalAIDiscordAgent.discord.listener;

import com.discord.LocalAIDiscordAgent.chatClient.service.ChatClientService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessSummaryClient;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import com.discord.LocalAIDiscordAgent.user.service.UserService;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
public abstract class MessageListener {

    private static final int MAX_INPUT_LENGTH = 4096;
    private static final int DISCORD_MAX_MESSAGE_LEN = 2000;

    public Mono<Void> processCommandAI(Message eventMessage, DiscGlobalData discGlobalData, UserService userService, ChatClientService chatClientService, ProcessSummaryClient processSummaryClient) {
        return Mono.just(eventMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .flatMap(message -> {

                    UserEntity user = userService.getUser();

                    if (user == null) {
                        user = userService.buildUser();
                        userService.saveUser(user);
                    }else if (!discGlobalData.getServerNickname().equals(user.getServerNickname())){
                        userService.updateUser(user, discGlobalData.getServerNickname());
                    }

                    UserEntity finalUser = user;

                    Mono<String> responseMono =
                            Mono.fromCallable(() -> chatClientService.generateLLMResponse(finalUser))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .cache();


                    return message.getChannel()
                            .flatMap(channel ->
                                    {
                                        try {
                                            return responseMono
                                                    .flatMapMany(response -> {
                                                        String cleanedResponse = cleanResponse(response);
                                                        List<String> chunks = splitIntoChunks(cleanedResponse, DISCORD_MAX_MESSAGE_LEN);
                                                        return Flux.fromIterable(chunks)
                                                                .concatMap(channel::createMessage);
                                                    })
                                                    .then(Mono.fromCallable(discGlobalData::getImagePath)
                                                            .subscribeOn(Schedulers.boundedElastic()))
                                                    .flatMap(imagePath -> sendImage(channel, imagePath, "Generated image for user %s".formatted(discGlobalData.getServerNickname())))
                                                    .then(Mono.fromRunnable(() -> {
                                                        try {
                                                            processSummaryClient.saveInteraction();
                                                        } catch (Exception e) {
                                                            log.warn("Failed to save interaction summary", e);
                                                        }
                                                    }));
                                        } catch (Exception e) {
                                            return Mono.error(new RuntimeException(e));
                                        }
                                    }
                            );

                }).then(Mono.fromRunnable(discGlobalData::setDiscTonull));
    }

    public Mono<Void> processCommand(Message eventMessage) {
        return Mono.just(eventMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(message -> message.getContent().equalsIgnoreCase("!todo"))
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("Things to do today:\n - write a bot\n - eat lunch\n - play a game"))
                .then();
    }

    private Mono<Void> sendImage(MessageChannel channel, Path imagePath, String caption) {
        if (imagePath == null || !Files.exists(imagePath)) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> Files.newInputStream(imagePath))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(inputStream ->
                        channel.createMessage(spec -> {
                            spec.setContent(caption);
                            spec.addFile(imagePath.getFileName().toString(), inputStream);
                        }).doFinally(signal -> {
                            try {
                                inputStream.close();
                            } catch (Exception ignored) {
                            }
                        })
                )
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
