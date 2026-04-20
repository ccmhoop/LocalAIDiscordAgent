package com.discord.LocalAIDiscordAgent.discord.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalDataContextHolder;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalDataService;
import com.discord.LocalAIDiscordAgent.discord.listener.EventListenerINTF;
import com.discord.LocalAIDiscordAgent.discord.listener.MessageListener;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessSummaryClient;
import com.discord.LocalAIDiscordAgent.llmClients.chatClient.service.ChatClientService;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.llmCallChains.LLMCallChain;
import com.discord.LocalAIDiscordAgent.user.service.UserService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.http.client.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class MessageCreateListenerINTF extends MessageListener implements EventListenerINTF<MessageCreateEvent> {

    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration PROCESS_RETRY_FIRST_BACKOFF = Duration.ofSeconds(2);
    private static final Duration PROCESS_RETRY_MAX_BACKOFF = Duration.ofSeconds(20);

    private static final int PROCESS_RETRY_COUNT = 3;

    private final UserService userService;
    private final LLMCallChain llmCallChain;
    private final ChatClientService chatClientService;
    private final ProcessSummaryClient processSummaryClient;
    private final DiscGlobalDataService discGlobalDataService;
    private final DiscordRequestQueueService discordRequestQueueService;

    public MessageCreateListenerINTF(
            UserService userService, LLMCallChain llmCallChain,
            ChatClientService chatClientService,
            ProcessSummaryClient processSummaryClient,
            DiscGlobalDataService discGlobalDataService,
            DiscordRequestQueueService discordRequestQueueService
    ) {
        this.userService = userService;
        this.llmCallChain = llmCallChain;
        this.chatClientService = chatClientService;
        this.processSummaryClient = processSummaryClient;
        this.discGlobalDataService = discGlobalDataService;
        this.discordRequestQueueService = discordRequestQueueService;
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        return Mono.defer(() -> {
            Message message = event.getMessage();

            if (message.getAuthor().map(User::isBot).orElse(false)) {
                return Mono.empty();
            }

            Snowflake botId = event.getClient().getSelfId();

            if (!message.getUserMentionIds().contains(botId)) {
                return Mono.empty();
            }

            if (!startsWithBotMention(message.getContent(), botId)) {
                return Mono.empty();
            }

            DiscGlobalData discGlobalData = discGlobalDataService.build(event);

            if (!discGlobalData.isValid()) {
                return Mono.empty();
            }

            if (discGlobalData.hasEmptyPrompt()) {
                return sendSafely(
                        message,
                        "You mentioned me, but your message is empty. Please add a question or instruction after the mention."
                );
            }

            String requestId = message.getId().asString();

            return discordRequestQueueService.enqueue(
                            requestId,
                            () -> Mono.defer(() ->
                                            processCommandAI(
                                                    message,
                                                    userService,
                                                    chatClientService,
                                                    processSummaryClient,
                                                    llmCallChain
                                            )
                                    )
                                    .timeout(PROCESS_TIMEOUT)
                                    .retryWhen(buildProcessRetry(requestId))
                                    .contextWrite(ctx -> DiscGlobalDataContextHolder.put(ctx, discGlobalData))
                    )
                    .doOnSubscribe(sub -> log.info(
                            "Started Discord AI request. requestId={}, messageId={}",
                            requestId,
                            message.getId().asString()
                    ))
                    .doOnSuccess(unused -> log.info(
                            "Completed Discord AI request. requestId={}, messageId={}",
                            requestId,
                            message.getId().asString()
                    ))
                    .doOnError(error -> log.error(
                            "Failed Discord AI request. requestId={}, messageId={}",
                            requestId,
                            message.getId().asString(),
                            error
                    ))
                    .onErrorResume(error -> sendSafely(
                            message,
                            "I hit a temporary error while processing that request. Please try again in a moment."
                    ))
                    .checkpoint("message-create-request-" + requestId);
        });
    }

    @Override
    public Mono<Void> handleError(Throwable error) {
        log.error("Unhandled error in MessageCreateListenerINTF", error);
        return Mono.empty();
    }

    private Retry buildProcessRetry(String requestId) {
        return Retry.backoff(PROCESS_RETRY_COUNT, PROCESS_RETRY_FIRST_BACKOFF)
                .maxBackoff(PROCESS_RETRY_MAX_BACKOFF)
                .jitter(0.5)
                .filter(this::isRetryableProcessError)
                .doBeforeRetry(signal -> log.warn(
                        "Retrying Discord AI request. requestId={}, attempt={}, cause={}",
                        requestId,
                        signal.totalRetries() + 1,
                        signal.failure().toString()
                ));
    }

    private boolean isRetryableProcessError(Throwable error) {
        if (error instanceof TimeoutException || error instanceof IOException) {
            return true;
        }

        if (error instanceof ClientException clientException) {
            int statusCode = clientException.getStatus().code();
            return statusCode >= 500;
        }

        return false;
    }

    private Mono<Void> sendSafely(Message message, String responseText) {
        return message.getChannel()
                .flatMap(channel -> channel.createMessage(responseText))
                .timeout(SEND_TIMEOUT)
                .doOnError(error -> log.warn(
                        "Failed to send Discord response for messageId={}",
                        message.getId().asString(),
                        error
                ))
                .onErrorResume(error -> Mono.empty())
                .then();
    }

    private boolean startsWithBotMention(String content, Snowflake botId) {
        if (content == null || content.isBlank()) {
            return false;
        }

        String trimmed = content.trim();
        String mention1 = "<@" + botId.asString() + ">";
        String mention2 = "<@!" + botId.asString() + ">";

        return trimmed.startsWith(mention1) || trimmed.startsWith(mention2);
    }
}