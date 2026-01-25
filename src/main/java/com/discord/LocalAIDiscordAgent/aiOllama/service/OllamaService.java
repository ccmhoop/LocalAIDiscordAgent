package com.discord.LocalAIDiscordAgent.aiOllama.service;

import com.discord.LocalAIDiscordAgent.aiSystemMsgBuilder.service.SystemMsgService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OllamaService {

    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(120);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final ChatClient scottishChatClient;
    private final ChatClient scottishToolChatClient;
    private final SystemMsgService systemMsgService;

    public OllamaService(
            @Qualifier("scottishChatClient") ChatClient scottishChatClient,
            @Qualifier("scottishToolChatClient") ChatClient scottishToolChatClient,
            SystemMsgService systemMsgService
    ) {
        this.scottishChatClient = scottishChatClient;
        this.scottishToolChatClient = scottishToolChatClient;
        this.systemMsgService = systemMsgService;
    }

    public Mono<String> generateScottishResponseMono(String userMessage, String userId, String guildId, String channelId) {

        String conversationId = buildConversationId(userId, guildId, channelId);

        boolean useTools = shouldUseTools(userMessage);
        ChatClient client = useTools ? scottishToolChatClient : scottishChatClient;

        List<Message> messages = useTools
                ? List.of(new UserMessage(userMessage))
                : systemMsgService.systemMsg(userId, userMessage);

        return Mono.defer(() -> Mono.fromCallable(() -> {
                    String txt = extractTextResponse(
                            client.prompt()
                                    .advisors(a -> {
                                        if (!useTools) {
                                            a.param(ChatMemory.CONVERSATION_ID, conversationId);
                                        }
                                    })
                                    .messages(messages)
                                    .call()
                                    .chatResponse()
                    );

                    txt = (txt == null) ? "" : txt.trim();

                    // Treat blank output as a retryable failure (prevents "send prompt twice")
                    if (txt.isBlank()) {
                        throw new BlankModelResponseException("Model returned blank output");
                    }

                    return txt;
                }))
                .subscribeOn(Schedulers.boundedElastic()) // call() blocks
                .timeout(RESPONSE_TIMEOUT)

                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS - 1, Duration.ofSeconds(1))
                        .filter(this::isRetryable))

                .doOnNext(txt -> log.debug("Ollama response (conversationId={}, useTools={}): {}", conversationId, useTools, txt))

                .onErrorResume(BlankModelResponseException.class, e ->
                        Mono.just("I received information but couldn't formulate a response. Please try asking in a different way.")
                )

                .onErrorResume(e -> {
                    log.warn("Ollama failed (conversationId={}, useTools={}): {}", conversationId, useTools, e.toString(), e);
                    return Mono.just("I had a problem generating a response. Please try again.");
                });
    }

    private String extractTextResponse(Object chatResponse) {
        if (chatResponse == null) {
            return "";
        }
        try {
            var cr = (org.springframework.ai.chat.model.ChatResponse) chatResponse;
            if (cr.getResult() == null || cr.getResult().getOutput() == null) {
                return "";
            }
            return Objects.toString(cr.getResult().getOutput().getText(), "");
        } catch (ClassCastException e) {
            return "";
        }
    }

    private boolean shouldUseTools(String userMessage) {
        if (userMessage == null) return false;
        String t = userMessage.toLowerCase();

        return t.contains("search online")
                || t.contains("websearch")
                || t.contains("google")
                || t.contains("bing")
                || t.contains("source")
                || t.contains("sources")
                || t.contains("link")
                || t.contains("links")
                || t.contains("http://")
                || t.contains("https://")
                || t.contains("www.")
                || t.contains("lookup")
                || t.contains("look up")
                || t.contains("search");
    }

    private boolean isRetryable(Throwable e) {
        if (e instanceof BlankModelResponseException) return true;
        if (e instanceof TimeoutException) return true;
        if (e instanceof IOException) return true;

        String msg = e.toString().toLowerCase();
        return msg.contains("readtimeoutexception")
                || msg.contains("resourceaccessexception")
                || msg.contains("i/o error")
                || msg.contains("connection reset")
                || msg.contains("connection refused");
    }

    private String buildConversationId(String userId, String guildId, String channelId) {
        String safeGuild = (guildId == null || guildId.isBlank()) ? "dm" : guildId;
        String safeChannel = (channelId == null || channelId.isBlank()) ? "dm" : channelId;
        String safeUser = (userId == null || userId.isBlank()) ? "unknown-user" : userId;
        return safeGuild + ":" + safeChannel + ":" + safeUser;
    }

    private static final class BlankModelResponseException extends RuntimeException {
        private BlankModelResponseException(String message) {
            super(message);
        }
    }
}
