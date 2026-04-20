package com.discord.LocalAIDiscordAgent.discord.config;

import com.discord.LocalAIDiscordAgent.discord.listener.EventListenerINTF;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.http.client.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Configuration
public class BotConfiguration {

    @Value("${discord.token}")
    private String token;

    private static final Duration LOGIN_RETRY_FIRST_BACKOFF = Duration.ofSeconds(2);
    private static final Duration LOGIN_RETRY_MAX_BACKOFF = Duration.ofMinutes(1);

    private static final Duration EVENT_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration EVENT_RETRY_FIRST_BACKOFF = Duration.ofSeconds(2);
    private static final Duration EVENT_RETRY_MAX_BACKOFF = Duration.ofSeconds(20);

    private static final int EVENT_RETRY_COUNT = 3;
    private static final int EVENT_CONCURRENCY = 8;

    @Bean
    public GatewayDiscordClient gatewayDiscordClient(List<EventListenerINTF<? extends Event>> listeners) {
        GatewayDiscordClient discord = DiscordClientBuilder.create(token)
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.of(
                        Intent.GUILDS,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.MESSAGE_CONTENT,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_PRESENCES
                ))
                .login()
                .retryWhen(
                        Retry.backoff(5, LOGIN_RETRY_FIRST_BACKOFF)
                                .maxBackoff(LOGIN_RETRY_MAX_BACKOFF)
                                .jitter(0.5)
                                .filter(this::isRetryableLoginError)
                                .doBeforeRetry(signal -> log.warn(
                                        "Discord login retry {} because of {}",
                                        signal.totalRetries() + 1,
                                        signal.failure().toString()
                                ))
                )
                .block();

        if (discord == null) {
            throw new IllegalStateException("Failed to login to Discord gateway.");
        }

        for (EventListenerINTF<? extends Event> listener : listeners) {
            registerListener(discord, listener);
        }

        log.info("Discord gateway connected and listeners registered.");
        return discord;
    }

    private <T extends Event> Disposable registerListener(
            GatewayDiscordClient discord,
            EventListenerINTF<T> listener
    ) {
        return discord.on(listener.getEventType())
                .flatMap(event -> executeListenerSafely(listener, event), EVENT_CONCURRENCY)
                .subscribe(
                        unused -> {
                        },
                        error -> log.error(
                                "Listener stream terminated for {}",
                                listener.getClass().getSimpleName(),
                                error
                        )
                );
    }

    private <T extends Event> Mono<Void> executeListenerSafely(
            EventListenerINTF<T> listener,
            T event
    ) {
        return Mono.defer(() -> listener.execute(event))
                .timeout(EVENT_TIMEOUT)
                .retryWhen(
                        Retry.backoff(EVENT_RETRY_COUNT, EVENT_RETRY_FIRST_BACKOFF)
                                .maxBackoff(EVENT_RETRY_MAX_BACKOFF)
                                .jitter(0.5)
                                .filter(this::isRetryableListenerError)
                                .doBeforeRetry(signal -> log.warn(
                                        "Retrying listener={} event={} attempt={} cause={}",
                                        listener.getClass().getSimpleName(),
                                        event.getClass().getSimpleName(),
                                        signal.totalRetries() + 1,
                                        signal.failure().toString()
                                ))
                )
                .onErrorResume(error ->
                        Mono.defer(() -> listener.handleError(error))
                                .onErrorResume(handleErrorFailure -> {
                                    log.error(
                                            "Listener handleError also failed for {}",
                                            listener.getClass().getSimpleName(),
                                            handleErrorFailure
                                    );
                                    return Mono.empty();
                                })
                );
    }

    private boolean isRetryableLoginError(Throwable error) {
        return error instanceof IOException
                || error instanceof TimeoutException;
    }

    private boolean isRetryableListenerError(Throwable error) {
        if (error instanceof IOException || error instanceof TimeoutException) {
            return true;
        }

        if (error instanceof ClientException clientException) {
            int statusCode = clientException.getStatus().code();
            return statusCode >= 500;
        }

        return false;
    }
}
