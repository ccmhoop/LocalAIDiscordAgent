package com.discord.LocalAIDiscordAgent.discord.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

    private final ConcurrentMap<String, InternalAdmission> inFlightRequests = new ConcurrentHashMap<>();
    private final List<InternalAdmission> pendingQueue = new ArrayList<>();

    private final Object emitLock = new Object();
    private final Object queueLock = new Object();

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


    public Mono<QueueAdmission> enqueueWithPosition(String requestId, Supplier<Mono<Void>> taskSupplier) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(taskSupplier, "taskSupplier must not be null");

        if (!acceptingRequests.get()) {
            return Mono.error(new IllegalStateException("Discord request queue is not accepting requests"));
        }

        InternalAdmission existing = inFlightRequests.get(requestId);
        if (existing != null) {
            log.info("Request already queued/running [{}] - joining existing execution", requestId);
            return Mono.just(existing.publicView());
        }

        InternalAdmission admission = new InternalAdmission(requestId);

        synchronized (queueLock) {
            InternalAdmission doubleCheck = inFlightRequests.get(requestId);
            if (doubleCheck != null) {
                log.info("Request already queued/running [{}] - joining existing execution", requestId);
                return Mono.just(doubleCheck.publicView());
            }

            inFlightRequests.put(requestId, admission);
            pendingQueue.add(admission);
            refreshQueuePositionsLocked();
        }

        QueuedRequest request = new QueuedRequest(requestId, taskSupplier, admission);

        Sinks.EmitResult emitResult;
        synchronized (emitLock) {
            emitResult = sink.tryEmitNext(request);
        }

        if (emitResult.isFailure()) {
            synchronized (queueLock) {
                pendingQueue.remove(admission);
                inFlightRequests.remove(requestId, admission);
                refreshQueuePositionsLocked();
            }

            IllegalStateException enqueueError =
                    new IllegalStateException("Failed to enqueue request [" + requestId + "]: " + emitResult);

            admission.positionSink().tryEmitError(enqueueError);
            admission.startedSink().tryEmitError(enqueueError);
            admission.completionSink().tryEmitError(enqueueError);

            return Mono.error(enqueueError);
        }

        log.info("Queued request [{}] at current position {}", requestId, admission.currentPosition());
        return Mono.just(admission.publicView());
    }

    private Mono<Void> processRequest(QueuedRequest request) {
        return Mono.defer(() -> {
                    synchronized (queueLock) {
                        pendingQueue.remove(request.admission());
                        refreshQueuePositionsLocked();
                    }

                    request.admission().startedSink().tryEmitEmpty();
                    request.admission().positionSink().tryEmitComplete();

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
                    request.admission().completionSink().tryEmitEmpty();
                })
                .doOnError(error -> {
                    if (error instanceof TimeoutException) {
                        log.error("Queued request timed out [{}]", request.requestId(), error);
                    } else {
                        log.error("Queued request failed [{}]", request.requestId(), error);
                    }
                    request.admission().completionSink().tryEmitError(error);
                })
                .doFinally(signalType -> {
                    inFlightRequests.remove(request.requestId(), request.admission());
                })
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

    private void refreshQueuePositionsLocked() {
        for (int i = 0; i < pendingQueue.size(); i++) {
            InternalAdmission admission = pendingQueue.get(i);
            int newPosition = i + 1;

            if (admission.currentPosition() != newPosition) {
                admission.currentPosition(newPosition);
                admission.positionSink().tryEmitNext(newPosition);
            }
        }
    }

    private void failAllPending(Throwable error) {
        synchronized (queueLock) {
            for (InternalAdmission admission : inFlightRequests.values()) {
                log.warn("Failing pending request [{}] because queue is stopping/stopped", admission.requestId());
                admission.positionSink().tryEmitError(error);
                admission.startedSink().tryEmitError(error);
                admission.completionSink().tryEmitError(error);
            }

            pendingQueue.clear();
            inFlightRequests.clear();
        }
    }

    public record QueueAdmission(
            String requestId,
            int initialPosition,
            Flux<Integer> positionUpdates,
            Mono<Void> started,
            Mono<Void> completion
    ) {}

    private static final class InternalAdmission {
        private final String requestId;
        private final Sinks.Many<Integer> positionSink;
        private final Sinks.One<Void> startedSink;
        private final Sinks.One<Void> completionSink;
        private volatile int currentPosition;

        private InternalAdmission(String requestId) {
            this.requestId = requestId;
            this.positionSink = Sinks.many().replay().latest();
            this.startedSink = Sinks.one();
            this.completionSink = Sinks.one();
            this.currentPosition = 0;
        }

        private QueueAdmission publicView() {
            return new QueueAdmission(
                    requestId,
                    currentPosition,
                    positionSink.asFlux().distinctUntilChanged(),
                    startedSink.asMono(),
                    completionSink.asMono()
            );
        }

        private String requestId() {
            return requestId;
        }

        private Sinks.Many<Integer> positionSink() {
            return positionSink;
        }

        private Sinks.One<Void> startedSink() {
            return startedSink;
        }

        private Sinks.One<Void> completionSink() {
            return completionSink;
        }

        private int currentPosition() {
            return currentPosition;
        }

        private void currentPosition(int currentPosition) {
            this.currentPosition = currentPosition;
        }
    }

    private record QueuedRequest(
            String requestId,
            Supplier<Mono<Void>> taskSupplier,
            InternalAdmission admission
    ) {}
}