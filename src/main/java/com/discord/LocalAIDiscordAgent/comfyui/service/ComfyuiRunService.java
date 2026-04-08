package com.discord.LocalAIDiscordAgent.comfyui.service;

import com.discord.LocalAIDiscordAgent.comfyui.imageGenerator.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.records.MusicSettingsRecord;
import com.discord.LocalAIDiscordAgent.comfyui.videoGenerator.records.VideoSettingsRecord;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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


    public Path generateVideo(PromptData promptData) throws Exception {
        VideoSettingsRecord imageSettings = promptData.getVideoSettings();
        ClassPathResource workflowResource = new ClassPathResource("comfyui/video_api_wan2.2.json");

        if (!workflowResource.exists()) {
            throw new IllegalStateException("Workflow file not found in classpath: comfyui/video_api_wan2.2.json");
        }

        Map<String, Object> apiWorkflow;
        try (var inputStream = workflowResource.getInputStream()) {
            apiWorkflow = objectMapper.readValue(
                    inputStream,
                    new TypeReference<Map<String, Object>>() {}
            );
        }

        setPromptText(apiWorkflow, "72", imageSettings.negativePrompt());
        setPromptText(apiWorkflow, "89", imageSettings.positivePrompt());


        byte[] imageBytes = comfyuiService.runGenerationWorkflow(apiWorkflow);

        String fileName = UUID.randomUUID() + ".mp4";
        Path outputPath = imageRoot.resolve(fileName).normalize();

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        Files.write(outputPath, imageBytes);

        return outputPath;
    }

    public Path generateImage(PromptData promptData) throws Exception {
        ImageSettingsRecord imageSettings = promptData.getImageSettings();
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

        byte[] imageBytes = comfyuiService.runGenerationWorkflow(apiWorkflow);

        String fileName = UUID.randomUUID() + ".png";
        Path outputPath = imageRoot.resolve(fileName).normalize();

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        Files.write(outputPath, imageBytes);

        return outputPath;
    }

    public Path generateMusic(PromptData promptData) throws Exception {
        MusicSettingsRecord musicSettings = promptData.getMusicSettings();
        ClassPathResource workflowResource = new ClassPathResource("comfyui/music_gen_api.json");

        if (!workflowResource.exists()) {
            throw new IllegalStateException("Workflow file not found in classpath: music_gen_api.json");
        }

        Map<String, Object> apiWorkflow;
        try (var inputStream = workflowResource.getInputStream()) {
            apiWorkflow = objectMapper.readValue(
                    inputStream,
                    new TypeReference<Map<String, Object>>() {}
            );
        }

        setMusicSettings(apiWorkflow, "94", musicSettings);

        byte[] audioBytes = comfyuiService.runGenerationWorkflow(apiWorkflow);

        String fileName = musicSettings.title() + " "+ UUID.randomUUID() + ".mp3".trim();
        Path outputPath = imageRoot.resolve(fileName).normalize();

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        Files.write(outputPath, audioBytes);

        return outputPath;
    }


    private void setMusicSettings(Map<String, Object> workflow, String nodeId, MusicSettingsRecord musicSettings) {
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


        inputs.put("keyscale", "C major");
        inputs.put("lyrics", musicSettings.lyrics());
        inputs.put("bpm", musicSettings.bpm());
        inputs.put("tags", musicSettings.tags());
    }

    private String normalizeKeyScale(String keyScale) {
        if (keyScale == null || keyScale.isBlank()) {
            return "C major";
        }

        return switch (keyScale.toLowerCase().trim()) {
            case "c" -> "C major";
            case "d" -> "D major";
            case "e" -> "E major";
            case "f" -> "F major";
            case "g" -> "G major";
            case "a" -> "A major";
            case "b" -> "B major";
            case "cm" -> "C minor";
            case "dm" -> "D minor";
            case "em" -> "E minor";
            case "fm" -> "F minor";
            case "gm" -> "G minor";
            case "am" -> "A minor";
            case "bm" -> "B minor";
            default -> keyScale;
        };
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
