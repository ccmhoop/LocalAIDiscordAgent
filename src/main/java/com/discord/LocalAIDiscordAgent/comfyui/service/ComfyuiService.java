package com.discord.LocalAIDiscordAgent.comfyui.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ComfyuiService {

    private final RestClient restClient;

    public ComfyuiService(RestClient restClient) {
        this.restClient = restClient;
    }

    public byte[] runWorkflowAndGetFirstImage(Map<String, Object> apiWorkflow) throws InterruptedException {
        String clientId = UUID.randomUUID().toString();
        String promptId = UUID.randomUUID().toString();

        // 1) Queue the prompt
        QueuePromptRequest request = new QueuePromptRequest(apiWorkflow, clientId, promptId);

        QueuePromptResponse response = restClient.post()
                .uri("/prompt")
                .body(request)
                .retrieve()
                .body(QueuePromptResponse.class);

        if (response == null || response.prompt_id() == null) {
            throw new IllegalStateException("ComfyUI did not return a prompt_id");
        }

        // 2) Poll history until finished
        Map<String, Object> historyEntry = waitForHistory(promptId, Duration.ofMinutes(10));

        // 3) Extract first image reference
        ImageRef imageRef = extractFirstImage(historyEntry);
        if (imageRef == null) {
            throw new IllegalStateException("No image found in ComfyUI history for prompt_id=" + promptId);
        }

        // 4) Download raw image bytes from /view
        URI imageUri = UriComponentsBuilder.fromPath("/view")
                .queryParam("filename", imageRef.filename())
                .queryParam("subfolder", imageRef.subfolder())
                .queryParam("type", imageRef.type())
                .build(true)
                .toUri();

        byte[] imageBytes = restClient.get()
                .uri(imageUri)
                .retrieve()
                .body(byte[].class);

        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalStateException("Downloaded image is empty");
        }

        return imageBytes;
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

    private ImageRef extractFirstImage(Map<String, Object> historyEntry) {
        Object outputsObj = historyEntry.get("outputs");
        if (!(outputsObj instanceof Map<?, ?> outputs)) {
            return null;
        }

        for (Object nodeOutputObj : outputs.values()) {
            if (!(nodeOutputObj instanceof Map<?, ?> nodeOutput)) {
                continue;
            }

            Object imagesObj = nodeOutput.get("images");
            if (!(imagesObj instanceof List<?> images) || images.isEmpty()) {
                continue;
            }

            Object firstImageObj = images.get(0);
            if (!(firstImageObj instanceof Map<?, ?> img)) {
                continue;
            }

            String filename = asString(img.get("filename"));
            String subfolder = asString(img.get("subfolder"));
            String type = asString(img.get("type"));

            if (filename != null && type != null) {
                return new ImageRef(filename, subfolder == null ? "" : subfolder, type);
            }
        }

        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public record QueuePromptRequest(
            Map<String, Object> prompt,
            String client_id,
            String prompt_id
    ) {}

    public record QueuePromptResponse(
            String prompt_id,
            Double number,
            Map<String, Object> node_errors
    ) {}

    public record ImageRef(
            String filename,
            String subfolder,
            String type
    ) {}

}
