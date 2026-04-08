package com.discord.LocalAIDiscordAgent.comfyui.videoGenerator.service;

import com.discord.LocalAIDiscordAgent.comfyui.helpers.ComfyHelper;
import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.comfyui.videoGenerator.records.VideoSettingsRecord;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class VideoGenerationService {

    private final ObjectMapper objectMapper;
    private final ComfyuiService comfyuiService;
    private final Path FILE_ROOT = Path.of("output/video");

    public VideoGenerationService(ObjectMapper objectMapper, ComfyuiService comfyuiService) {
        this.objectMapper = objectMapper;
        this.comfyuiService = comfyuiService;
    }

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

        setVideoSettings(apiWorkflow, imageSettings);

        byte[] imageBytes = comfyuiService.runGenerationWorkflow(apiWorkflow);

        String fileName = UUID.randomUUID() + ".mp4";
        Path outputPath = FILE_ROOT.resolve(fileName).normalize();

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        Files.write(outputPath, imageBytes);

        return outputPath;
    }

    private void setVideoSettings(Map<String, Object> workflow, VideoSettingsRecord settings) {
        Map<String, Object> inputs = ComfyHelper.getStringObjectMap(workflow, "72");
        inputs.put("text", settings.negativePrompt());
        inputs = ComfyHelper.getStringObjectMap(workflow, "89");
        inputs.put("text", settings.positivePrompt());
    }
}
