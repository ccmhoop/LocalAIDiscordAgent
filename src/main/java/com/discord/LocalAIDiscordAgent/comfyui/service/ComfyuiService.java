package com.discord.LocalAIDiscordAgent.comfyui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class ComfyuiService {

    private final RestClient restClient;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${comfyui.ws-base-url}")
    private String wsBaseUrl;

    public ComfyuiService(RestClient restClient, HttpClient httpClient) {
        this.restClient = restClient;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    public byte[] runGenerationWorkflow(Map<String, Object> apiWorkflow) {
        String clientId = UUID.randomUUID().toString();
        AtomicReference<String> promptIdRef = new AtomicReference<>();
        CompletableFuture<Void> finishedFuture = new CompletableFuture<>();

        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(
                        URI.create(wsBaseUrl + "/ws?clientId=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)),
                        new WebSocket.Listener() {
                            private final StringBuilder textBuffer = new StringBuilder();

                            @Override
                            public void onOpen(WebSocket webSocket) {
                                log.debug("Connected to ComfyUI websocket");
                                webSocket.request(1);
                            }

                            @Override
                            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                                textBuffer.append(data);

                                if (last) {
                                    try {
                                        JsonNode message = objectMapper.readTree(textBuffer.toString());
                                        handleWebsocketMessage(message, promptIdRef, finishedFuture);
                                    } catch (Exception e) {
                                        log.error("Failed to parse ComfyUI websocket message", e);
                                        if (!finishedFuture.isDone()) {
                                            finishedFuture.completeExceptionally(e);
                                        }
                                    } finally {
                                        textBuffer.setLength(0);
                                    }
                                }

                                webSocket.request(1);
                                return CompletableFuture.completedFuture(null);
                            }

                            @Override
                            public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
                                webSocket.request(1);
                                return CompletableFuture.completedFuture(null);
                            }

                            @Override
                            public void onError(WebSocket webSocket, Throwable error) {
                                log.error("ComfyUI websocket error", error);
                                if (!finishedFuture.isDone()) {
                                    finishedFuture.completeExceptionally(error);
                                }
                            }

                            @Override
                            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                                log.debug("ComfyUI websocket closed. status={}, reason={}", statusCode, reason);
                                return CompletableFuture.completedFuture(null);
                            }
                        }
                )
                .join();

        try {
            QueuePromptRequest request = new QueuePromptRequest(apiWorkflow, clientId);

            QueuePromptResponse response = restClient.post()
                    .uri("/prompt")
                    .body(request)
                    .retrieve()
                    .body(QueuePromptResponse.class);

            if (response == null || response.prompt_id() == null || response.prompt_id().isBlank()) {
                throw new IllegalStateException("ComfyUI did not return a prompt_id");
            }

            if (response.node_errors() != null && !response.node_errors().isEmpty()) {
                log.warn("ComfyUI returned node_errors: {}", response.node_errors());
            }

            String promptId = response.prompt_id();
            promptIdRef.set(promptId);

            log.info("Queued ComfyUI prompt {}", promptId);

            finishedFuture.orTimeout(10, TimeUnit.MINUTES).join();

            Map<String, Object> historyEntry = fetchHistoryEntry(promptId);

            FileRef fileRef = extractFirstFile(historyEntry);
            if (fileRef == null) {
                throw new IllegalStateException("No output file found in ComfyUI history for prompt_id=" + promptId);
            }

            URI fileUri = UriComponentsBuilder.fromPath("/view")
                    .queryParam("filename", fileRef.filename())
                    .queryParam("subfolder", fileRef.subfolder())
                    .queryParam("type", fileRef.type())
                    .build(true)
                    .toUri();

            byte[] fileBytes = restClient.get()
                    .uri(fileUri)
                    .retrieve()
                    .body(byte[].class);

            if (fileBytes == null || fileBytes.length == 0) {
                throw new IllegalStateException("Downloaded file is empty");
            }

            log.info("Downloaded ComfyUI output for prompt {} ({} bytes)", promptId, fileBytes.length);
            return fileBytes;

        } finally {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
            } catch (Exception e) {
                log.debug("Ignoring websocket close error", e);
            }
        }
    }

    private void handleWebsocketMessage(
            JsonNode message,
            AtomicReference<String> promptIdRef,
            CompletableFuture<Void> finishedFuture
    ) {
        String type = message.path("type").asText();
        JsonNode data = message.path("data");

        if ("execution_error".equals(type)) {
            String promptId = data.path("prompt_id").asText(null);
            String exceptionMessage = data.path("exception_message").asText("Unknown execution error");

            if (promptId != null && promptId.equals(promptIdRef.get()) && !finishedFuture.isDone()) {
                finishedFuture.completeExceptionally(
                        new IllegalStateException("ComfyUI execution error: " + exceptionMessage)
                );
            }
            return;
        }

        if ("executing".equals(type)) {
            String promptId = data.path("prompt_id").asText(null);
            JsonNode node = data.get("node");

            if (promptId != null
                    && promptId.equals(promptIdRef.get())
                    && (node == null || node.isNull())
                    && !finishedFuture.isDone()) {
                log.info("ComfyUI workflow finished for prompt {}", promptId);
                finishedFuture.complete(null);
            }
        }
    }

    private Map<String, Object> fetchHistoryEntry(String promptId) {
        Map<String, Object> history = restClient.get()
                .uri("/history/{promptId}", promptId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (history == null || !history.containsKey(promptId)) {
            throw new IllegalStateException("No history found for prompt_id=" + promptId);
        }

        Object entry = history.get(promptId);
        if (!(entry instanceof Map<?, ?> rawMap)) {
            throw new IllegalStateException("Unexpected history format for prompt_id=" + promptId);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> typed = (Map<String, Object>) rawMap;
        return typed;
    }

    private FileRef extractFirstFile(Map<String, Object> historyEntry) {
        Object outputsObj = historyEntry.get("outputs");
        if (!(outputsObj instanceof Map<?, ?> outputs)) {
            return null;
        }

        for (Object nodeOutputObj : outputs.values()) {
            if (!(nodeOutputObj instanceof Map<?, ?> nodeOutput)) {
                continue;
            }

            FileRef ref = extractFromField(nodeOutput, "audio");
            if (ref != null) {
                return ref;
            }

            ref = extractFromField(nodeOutput, "images");
            if (ref != null) {
                return ref;
            }
        }

        return null;
    }

    private FileRef extractFromField(Map<?, ?> nodeOutput, String fieldName) {
        Object filesObj = nodeOutput.get(fieldName);
        if (!(filesObj instanceof List<?> files) || files.isEmpty()) {
            return null;
        }

        Object firstFileObj = files.get(0);
        if (!(firstFileObj instanceof Map<?, ?> file)) {
            return null;
        }

        String filename = asString(file.get("filename"));
        String subfolder = asString(file.get("subfolder"));
        String type = asString(file.get("type"));

        if (filename != null && !filename.isBlank() && type != null && !type.isBlank()) {
            return new FileRef(filename, subfolder == null ? "" : subfolder, type);
        }

        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public record QueuePromptRequest(
            Map<String, Object> prompt,
            String client_id
    ) {}

    public record QueuePromptResponse(
            String prompt_id,
            Double number,
            Map<String, Object> node_errors
    ) {}

    public record FileRef(
            String filename,
            String subfolder,
            String type
    ) {}
}