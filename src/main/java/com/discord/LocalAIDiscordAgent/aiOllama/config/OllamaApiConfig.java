package com.discord.LocalAIDiscordAgent.aiOllama.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class OllamaApiConfig {

    // Adjust these to your needs
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(60); // Increased from 30 to 60 seconds
    private static final Duration RESPONSE_TIMEOUT = Duration.ofMinutes(15); // Increased from 10 to 15 minutes
    private static final Duration READ_TIMEOUT = Duration.ofMinutes(15); // Explicit read timeout
    private static final Duration WRITE_TIMEOUT = Duration.ofMinutes(5); // Explicit write timeout
    private static final String OLLAMA_BASE_URL = "http://127.0.0.1:11434";

    // Maximum buffer size for responses (16MB)
    private static final int MAX_BUFFER_SIZE = 16 * 1024 * 1024;

    @Bean
    public OllamaApi ollamaBasicApiConfig() {
        ConnectionProvider provider = ConnectionProvider.builder("ollama-connection-pool")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(60))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(30))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                .responseTimeout(RESPONSE_TIMEOUT)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT.toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT.toSeconds(), TimeUnit.SECONDS)))
                .keepAlive(true);

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_BUFFER_SIZE))
                .build();

        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(OLLAMA_BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        return OllamaApi.builder()
                .webClientBuilder(webClientBuilder)
                .build();
    }
}
