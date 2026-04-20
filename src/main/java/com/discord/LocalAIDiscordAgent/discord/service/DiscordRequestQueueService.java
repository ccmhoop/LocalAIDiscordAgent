package com.discord.LocalAIDiscordAgent.discord.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Slf4j
@Service
public class DiscordRequestQueueService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(3);

    private final Sinks.Many<QueuedRequest> sink =
            Sinks.many().unicast().onBackpressureBuffer();

    private final ConcurrentMap<String, Sinks.One<Void>> inFlightRequests = new ConcurrentHashMap<>();

    private final Object emitLock = new Object();

    private final AtomicBoolean acceptingRequests = new AtomicBoolean(false);

    private Disposable worker;

    @PostConstruct
    public void start() {
        acceptingRequests.set(true);

        this.worker = sink.asFlux()
                .concatMap(this::processRequest)
                .subscribe(
                        unused -> {
                        },
                        error -> {
                            acceptingRequests.set(false);
                            log.error("Queue worker stopped unexpectedly", error);
                            failAllPending(error);
                        },
                        () -> {
                            acceptingRequests.set(false);
                            log.warn("Queue worker completed");
                            failAllPending(new CancellationException("Discord request queue stopped"));
                        }
                );

        log.info("Discord request queue started");
    }

    public Mono<Void> enqueue(String requestId, Supplier<Mono<Void>> taskSupplier) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(taskSupplier, "taskSupplier must not be null");

        if (!acceptingRequests.get()) {
            return Mono.error(new IllegalStateException("Discord request queue is not accepting requests"));
        }

        Sinks.One<Void> completion = Sinks.one();
        Sinks.One<Void> existing = inFlightRequests.putIfAbsent(requestId, completion);

        if (existing != null) {
            log.info("Request already queued/running [{}] - joining existing execution", requestId);
            return existing.asMono();
        }

        QueuedRequest request = new QueuedRequest(requestId, taskSupplier, completion);

        Sinks.EmitResult emitResult;
        synchronized (emitLock) {
            emitResult = sink.tryEmitNext(request);
        }

        if (emitResult.isFailure()) {
            inFlightRequests.remove(requestId, completion);
            return Mono.error(new IllegalStateException(
                    "Failed to enqueue request [" + requestId + "]: " + emitResult
            ));
        }

        log.info("Queued request [{}]", requestId);

        return completion.asMono()
                .doOnCancel(() ->
                        log.info("Caller stopped waiting for queued request [{}]", requestId)
                );
    }

    private Mono<Void> processRequest(QueuedRequest request) {
        return Mono.defer(() -> {
                    Mono<Void> task = request.taskSupplier().get();

                    if (task == null) {
                        return Mono.error(new IllegalStateException(
                                "Task supplier returned null for request [" + request.requestId() + "]"
                        ));
                    }

                    return task;
                })
                .timeout(REQUEST_TIMEOUT)
                .doOnSubscribe(sub ->
                        log.info("Processing queued request [{}]", request.requestId())
                )
                .doOnSuccess(unused -> {
                    log.info("Queued request completed [{}]", request.requestId());
                    request.completion().tryEmitEmpty();
                })
                .doOnError(error -> {
                    if (error instanceof TimeoutException) {
                        log.error("Queued request timed out [{}]", request.requestId(), error);
                    } else {
                        log.error("Queued request failed [{}]", request.requestId(), error);
                    }
                    request.completion().tryEmitError(error);
                })
                .doFinally(signalType ->
                        inFlightRequests.remove(request.requestId(), request.completion())
                )
                .onErrorResume(error -> Mono.empty());
    }

    @PreDestroy
    public void stop() {
        acceptingRequests.set(false);

        if (worker != null) {
            worker.dispose();
        }

        sink.tryEmitComplete();
        failAllPending(new CancellationException("Discord request queue stopped"));

        log.info("Discord request queue stopped");
    }

    private void failAllPending(Throwable error) {
        inFlightRequests.forEach((requestId, completion) -> {
            log.warn("Failing pending request [{}] because queue is stopping/stopped", requestId);
            completion.tryEmitError(error);
        });
        inFlightRequests.clear();
    }

    private record QueuedRequest(
            String requestId,
            Supplier<Mono<Void>> taskSupplier,
            Sinks.One<Void> completion
    ) {}
}