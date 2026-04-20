package com.discord.LocalAIDiscordAgent.discord.listener;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalDataContextHolder;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessSummaryClient;
import com.discord.LocalAIDiscordAgent.llmClients.chatClient.service.ChatClientService;
import com.discord.LocalAIDiscordAgent.llmRouteDecider.records.RouteDecision;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.llmCallChains.LLMCallChain;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import com.discord.LocalAIDiscordAgent.user.service.UserService;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageEditSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public abstract class MessageListener {

    private static final int DISCORD_MAX_MESSAGE_LEN = 2000;

    public Mono<Void> processCommandAI(
            Message eventMessage,
            UserService userService,
            ChatClientService chatClientService,
            ProcessSummaryClient processSummaryClient,
            LLMCallChain llmCallChain
    ) {
        return DiscGlobalDataContextHolder.get()
                .flatMap(discGlobalData ->
                        Mono.just(eventMessage)
                                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                                .flatMap(message -> {

                                    UserEntity user = userService.getUser(discGlobalData);

                                    if (user == null) {
                                        user = userService.buildUser(discGlobalData);
                                        userService.saveUser(user);
                                    } else if (!discGlobalData.getServerNickname().equals(user.getServerNickname())) {
                                        userService.updateUser(user, discGlobalData.getServerNickname());
                                    }

                                    UserEntity finalUser = user;
                                    RouteDecision decision = llmCallChain.decideRoute(discGlobalData);

                                    return switch (decision.mode()) {
                                        case TEXT -> handleTextCommand(
                                                message,
                                                finalUser,
                                                discGlobalData,
                                                chatClientService
                                        );
                                        case IMAGE -> handleGeneratedCommand(
                                                message,
                                                llmCallChain.executeImageChain(discGlobalData),
                                                "Queued your image generation request...",
                                                "Image generation complete."
                                        );
                                        case VIDEO -> handleGeneratedCommand(
                                                message,
                                                llmCallChain.executeVideoChain(discGlobalData),
                                                "Queued your video generation request...",
                                                "Video generation complete."
                                        );
                                        case MUSIC -> handleGeneratedCommand(
                                                message,
                                                llmCallChain.executeMusicChain(discGlobalData),
                                                "Queued your music generation request...",
                                                "Music generation complete."
                                        );
                                    };
                                })
                );
    }

    public Mono<Void> processCommand(Message eventMessage) {
        return Mono.just(eventMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(message -> message.getContent().equalsIgnoreCase("!todo"))
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("Things to do today:\n - write a bot\n - eat lunch\n - play a game"))
                .then();
    }

    private Mono<Void> handleTextCommand(
            Message message,
            UserEntity user,
            DiscGlobalData discGlobalData,
            ChatClientService chatClientService
    ) {
        return message.getChannel()
                .flatMap(channel ->
                        chatClientService.generateLLMResponse(user, discGlobalData)
                                .flatMapMany(response -> {
                                    String cleanedResponse = cleanResponse(response);
                                    List<String> chunks = splitIntoChunks(cleanedResponse, DISCORD_MAX_MESSAGE_LEN);
                                    return Flux.fromIterable(chunks)
                                            .concatMap(channel::createMessage);
                                })
                                .then()
                );
    }

    private Mono<Void> handleGeneratedCommand(
            Message commandMessage,
            Mono<ComfyuiService.GeneratedFile> generationMono,
            String queuedText,
            String doneText
    ) {
        return commandMessage.getChannel()
                .flatMap(channel -> channel.createMessage(queuedText)
                        .flatMap(statusMessage -> {
                            Mono<ComfyuiService.GeneratedFile> sharedGeneration = generationMono.cache();

                            Mono<Void> progressUpdates = Flux.interval(Duration.ofSeconds(10))
                                    .takeUntilOther(sharedGeneration.materialize())
                                    .concatMap(tick ->
                                            statusMessage.edit(
                                                            MessageEditSpec.builder()
                                                                    .contentOrNull(queuedText + " Still working... elapsed " + ((tick + 1) * 10) + "s")
                                                                    .build()
                                                    )
                                                    .onErrorResume(error -> Mono.empty())
                                    )
                                    .then();

                            Mono<Void> completionFlow = sharedGeneration
                                    .flatMap(file -> sendGeneratedFile(channel, file, doneText))
                                    .then(statusMessage.edit(
                                            MessageEditSpec.builder()
                                                    .contentOrNull(doneText)
                                                    .build()
                                    ))
                                    .then()
                                    .onErrorResume(error ->
                                            statusMessage.edit(
                                                            MessageEditSpec.builder()
                                                                    .contentOrNull("Generation failed: " + safeMessage(error))
                                                                    .build()
                                                    )
                                                    .then()
                                    );

                            return Mono.when(progressUpdates, completionFlow).then();
                        })
                );
    }

    private Mono<Void> sendGeneratedFile(
            MessageChannel channel,
            ComfyuiService.GeneratedFile file,
            String caption
    ) {
        if (file == null || file.bytes() == null || file.bytes().length == 0) {
            return Mono.empty();
        }

        String filename = resolveFilename(file);

        return Mono.fromCallable(() -> new ByteArrayInputStream(file.bytes()))
                .flatMap(inputStream ->
                        channel.createMessage(spec -> {
                            spec.setContent(caption);
                            spec.addFile(filename, inputStream);
                        }).publishOn(Schedulers.boundedElastic()).doFinally(signal -> {
                            try {
                                inputStream.close();
                            } catch (Exception ignored) {
                            }
                        })
                )
                .then();
    }

    private String resolveFilename(ComfyuiService.GeneratedFile file) {
        if (file.filename() != null && !file.filename().isBlank()) {
            return file.filename();
        }

        return switch (file.type() == null ? "" : file.type()) {
            case "output", "temp" -> "result.png";
            default -> "result.bin";
        };
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

    private String safeMessage(Throwable error) {
        String msg = error.getMessage();
        return (msg == null || msg.isBlank()) ? error.getClass().getSimpleName() : msg;
    }
}