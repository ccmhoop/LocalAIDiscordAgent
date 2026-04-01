package com.discord.LocalAIDiscordAgent.comfyui.service;

import com.discord.LocalAIDiscordAgent.comfyui.records.ImageSettingsRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class ComfyuiRunService {

    private final ComfyuiService comfyuiService;
    private final ObjectMapper objectMapper;
    private final Path imageRoot = Path.of("images");


    public ComfyuiRunService(ComfyuiService comfyuiService, ObjectMapper objectMapper) {
        this.comfyuiService = comfyuiService;
        this.objectMapper = objectMapper;
    }

//    public ResponseEntity<String> runWorkflow() throws Exception {
//        Path imagePath = generateImage();
//        return ResponseEntity.ok(imagePath.getFileName().toString());
//    }

    public Path generateImage(ImageSettingsRecord imageSettings) throws Exception {
        ClassPathResource workflowResource = new ClassPathResource("comfyui/sdxl_api.json");

        if (!workflowResource.exists()) {
            throw new IllegalStateException("Workflow file not found in classpath: comfyui/sdxl_api.json");
        }

        Map<String, Object> apiWorkflow;
        try (var inputStream = workflowResource.getInputStream()) {
            apiWorkflow = objectMapper.readValue(
                    inputStream,
                    new TypeReference<Map<String, Object>>() {}
            );
        }

        setImageSize(apiWorkflow,"5", imageSettings.pixelWidth(), imageSettings.pixelHeight() );
        setPromptText(apiWorkflow, "6", imageSettings.positivePrompt());
        setPromptText(apiWorkflow, "7", imageSettings.negativePrompt());

        byte[] imageBytes = comfyuiService.runWorkflowAndGetFirstImage(apiWorkflow);

        String fileName = UUID.randomUUID() + ".png";
        Path outputPath = imageRoot.resolve(fileName).normalize();

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        Files.write(outputPath, imageBytes);

        return outputPath;
    }

    private void setPromptText(Map<String, Object> workflow, String nodeId, String text) {
        Object nodeObj = workflow.get(nodeId);
        if (!(nodeObj instanceof Map<?, ?> nodeMapRaw)) {
            throw new IllegalStateException("Node not found: " + nodeId);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> nodeMap = (Map<String, Object>) nodeMapRaw;

        Object inputsObj = nodeMap.get("inputs");
        if (!(inputsObj instanceof Map<?, ?> inputsRaw)) {
            throw new IllegalStateException("Inputs not found for node: " + nodeId);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> inputs = (Map<String, Object>) inputsRaw;

        inputs.put("text", text);
    }

    private void setImageSize(Map<String, Object> workflow, String nodeId, int width, int height) {
        Object nodeObj = workflow.get(nodeId);
        if (!(nodeObj instanceof Map<?, ?> nodeMapRaw)) {
            throw new IllegalStateException("Node not found: " + nodeId);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> nodeMap = (Map<String, Object>) nodeMapRaw;

        Object inputsObj = nodeMap.get("inputs");
        if (!(inputsObj instanceof Map<?, ?> inputsRaw)) {
            throw new IllegalStateException("Inputs not found for node: " + nodeId);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> inputs = (Map<String, Object>) inputsRaw;

        inputs.put("width", width);
        inputs.put("height", height);
    }


    public ResponseEntity<Resource> getImage(@PathVariable String fileName) throws Exception {
        Path imagePath = imageRoot.resolve(fileName).normalize();

        // basic safety check so "../" cannot escape the folder
        if (!imagePath.startsWith(imageRoot)) {
            return ResponseEntity.badRequest().build();
        }

        if (!Files.exists(imagePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = toResource(imagePath);

        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private Resource toResource(Path path) throws MalformedURLException {
        return new UrlResource(path.toUri());
    }
}
