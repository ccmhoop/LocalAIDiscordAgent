package com.discord.LocalAIDiscordAgent.discord.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;

import java.util.function.Supplier;

@Slf4j
@Service
public class DiscordRequestQueueService {

    private final Sinks.Many<QueuedRequest> sink =
            Sinks.many().unicast().onBackpressureBuffer();

    private Disposable worker;

    @PostConstruct
    public void start() {
        this.worker = sink.asFlux()
                .concatMap(request ->
                        Mono.defer(() -> request.taskSupplier().get())
                                .doOnSubscribe(sub ->
                                        log.info("Processing queued request [{}]", request.requestId()))
                                .doOnSuccess(unused ->
                                        request.completion().success())
                                .doOnError(error -> {
                                    log.error("Queued request failed [{}]", request.requestId(), error);
                                    request.completion().error(error);
                                })
                                .onErrorResume(error -> Mono.empty())
                )
                .subscribe(
                        unused -> { },
                        error -> log.error("Queue worker stopped unexpectedly", error)
                );
    }

    public Mono<Void> enqueue(String requestId, Supplier<Mono<Void>> taskSupplier) {
        return Mono.create(completion -> {
            QueuedRequest request = new QueuedRequest(requestId, taskSupplier, completion);

            Sinks.EmitResult result = sink.tryEmitNext(request);

            if (result.isFailure()) {
                completion.error(new IllegalStateException(
                        "Failed to enqueue request [" + requestId + "]: " + result
                ));
                return;
            }

            log.info("Queued request [{}]", requestId);
        });
    }

    @PreDestroy
    public void stop() {
        if (worker != null) {
            worker.dispose();
        }
        sink.tryEmitComplete();
    }

    private record QueuedRequest(
            String requestId,
            Supplier<Mono<Void>> taskSupplier,
            MonoSink<Void> completion
    ) {}
}