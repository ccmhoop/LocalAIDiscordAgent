package com.discord.LocalAIDiscordAgent.discord.listener;

import com.discord.LocalAIDiscordAgent.chatClient.service.ChatClientService;
import com.discord.LocalAIDiscordAgent.chatClient.service.ToolClientService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscDataRecord;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessSummaryClient;
import com.discord.LocalAIDiscordAgent.textToSpeech.VoiceMain;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import com.discord.LocalAIDiscordAgent.user.service.UserService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey.*;

@Slf4j
@Component
public abstract class MessageListener {

    private static final int MAX_INPUT_LENGTH = 4096;
    private static final int DISCORD_MAX_MESSAGE_LEN = 2000;
    private Map<DiscDataKey, String> discDataMap;

    public Mono<Void> processCommandAI(Message eventMessage, DiscGlobalData discGlobalData, UserService userService, ChatClientService chatClientService, ToolClientService toolClientService, ProcessSummaryClient processSummaryClient) {
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
                            Mono.fromCallable(() -> {
                                        if (toolClientService.shouldUseWebSearch()) {
                                            return toolClientService.generateToolResponse(finalUser, true);
                                        } else if (toolClientService.shouldUseDirectLink()) {
                                            return toolClientService.generateToolResponse(finalUser, false);
                                        } else {
                                            return chatClientService.generateLLMResponse(finalUser);
                                        }
                                    })
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .cache();


                    return message.getChannel()
                            .flatMap(channel ->
                                    responseMono
                                            .flatMapMany(response -> {
                                                String cleanedResponse = cleanResponse(response);
                                                List<String> chunks = splitIntoChunks(cleanedResponse, DISCORD_MAX_MESSAGE_LEN);
//                                                generateAndSaveAudio(cleanedResponse, userId);
//                                                VoiceMain.generateAndSaveAudio(cleanedResponse, discGlobalData.getUserId());

                                                return Flux.fromIterable(chunks)
                                                        .concatMap(chunk -> channel.createMessage(chunk)
                                                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(120))
                                                                        .filter(MessageListener::isConnectionReset)
                                                                        .doBeforeRetry(rs -> log.warn(
                                                                                "Retrying after connection reset, attempt: {}",
                                                                                rs.totalRetries() + 1
                                                                        ))));
                                            })
                                            .then(Mono.fromRunnable(() -> {
                                                try {
                                                    processSummaryClient.saveInteraction();
                                                } catch (ObjectOptimisticLockingFailureException e) {
                                                    log.warn("Failed to save interaction summary due to concurrent modification for conversation: {}:{}",
                                                            discGlobalData.getGuildId(), discGlobalData.getUserId(), e);
                                                    // Don't propagate this error as it's not critical to the user experience
                                                } catch (Exception e) {
                                                    log.error("Unexpected error saving interaction summary for conversation: {}:{}",
                                                            discGlobalData.getGuildId(), discGlobalData.getUserId(), e);
                                                }
                                            }))

                            );

                }).then();
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
