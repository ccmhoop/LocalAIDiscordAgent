package com.discord.LocalAIDiscordAgent.comfyui.imageGenerator.service;

import com.discord.LocalAIDiscordAgent.comfyui.helpers.ComfyHelper;
import com.discord.LocalAIDiscordAgent.comfyui.imageGenerator.records.ImageSettingsRecord;
import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageGenerationService {

    private final ObjectMapper objectMapper;
    private final ComfyuiService comfyuiService;
    private static final Path FILE_ROOT = Path.of("output/image");

    public ImageGenerationService(ObjectMapper objectMapper, ComfyuiService comfyuiService) {
        this.objectMapper = objectMapper;
        this.comfyuiService = comfyuiService;
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

        setImageSettings(apiWorkflow, imageSettings);

        byte[] imageBytes = comfyuiService.runGenerationWorkflow(apiWorkflow);

        String fileName = UUID.randomUUID() + ".png";
        Path outputPath = FILE_ROOT.resolve(fileName).normalize();

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        Files.write(outputPath, imageBytes);

        return outputPath;
    }

    private void setImageSettings(Map<String, Object> workflow, ImageSettingsRecord settings) {
        Map<String, Object> inputs = ComfyHelper.getStringObjectMap(workflow, "5");
        inputs.put("width", settings.pixelWidth());
        inputs.put("height", settings.pixelHeight());
        inputs = ComfyHelper.getStringObjectMap(workflow, "6");
        inputs.put("text", settings.positivePrompt());
        inputs = ComfyHelper.getStringObjectMap(workflow, "7");
        inputs.put("text", settings.negativePrompt());
    }

    public ResponseEntity<Resource> getImage(@PathVariable String fileName) throws Exception {
        Path imagePath = FILE_ROOT.resolve(fileName).normalize();

        // basic safety check so "../" cannot escape the folder
        if (!imagePath.startsWith(FILE_ROOT)) {
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
