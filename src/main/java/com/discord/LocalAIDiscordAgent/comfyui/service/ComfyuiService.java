package com.discord.LocalAIDiscordAgent.comfyui.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ComfyuiService {

    private final RestClient restClient;

    public ComfyuiService(RestClient restClient) {
        this.restClient = restClient;
    }

    public byte[] runGenerationWorkflow(Map<String, Object> apiWorkflow) throws InterruptedException {
        String clientId = UUID.randomUUID().toString();

        QueuePromptRequest request = new QueuePromptRequest(apiWorkflow, clientId);

        QueuePromptResponse response = restClient.post()
                .uri("/prompt")
                .body(request)
                .retrieve()
                .body(QueuePromptResponse.class);

        if (response == null || response.prompt_id() == null) {
            throw new IllegalStateException("ComfyUI did not return a prompt_id");
        }

        String promptId = response.prompt_id();

        Map<String, Object> historyEntry = waitForHistory(promptId, Duration.ofMinutes(10));

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

        return fileBytes;
    }

    private Map<String, Object> waitForHistory(String promptId, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> history = restClient.get()
                    .uri("/history/{promptId}", promptId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (history != null && history.containsKey(promptId)) {
                Object entry = history.get(promptId);
                if (entry instanceof Map<?, ?> rawMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typed = (Map<String, Object>) rawMap;
                    return typed;
                }
            }

            Thread.sleep(1000);
        }

        throw new IllegalStateException("Timed out waiting for ComfyUI prompt to finish: " + promptId);
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
            if (ref != null) return ref;

            ref = extractFromField(nodeOutput, "images");
            if (ref != null) return ref;
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

        if (filename != null && type != null) {
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
