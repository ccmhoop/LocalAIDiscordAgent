package com.discord.LocalAIDiscordAgent.comfyui.generators.videoGenerator.service;

import com.discord.LocalAIDiscordAgent.comfyui.generators.videoGenerator.payloadRecord.VideoSettingsPayload;
import com.discord.LocalAIDiscordAgent.comfyui.helpers.ComfyHelper;
import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.UUID;

@Service
public class VideoGenerationService {

    private final ObjectMapper objectMapper;
    private final ComfyuiService comfyuiService;

    public VideoGenerationService(ObjectMapper objectMapper, ComfyuiService comfyuiService) {
        this.objectMapper = objectMapper;
        this.comfyuiService = comfyuiService;
    }

    public Mono<ComfyuiService.GeneratedFile> generateVideo(PromptData promptData) {
        return Mono.fromCallable(() -> buildWorkflow(promptData))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(comfyuiService::runGenerationWorkflow)
                .map(this::withVideoFilename);
    }

    private Map<String, Object> buildWorkflow(PromptData promptData) throws Exception {
        VideoSettingsPayload videoSettings = promptData.getVideoSettings();
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

        setVideoSettings(apiWorkflow, videoSettings);
        return apiWorkflow;
    }

    private void setVideoSettings(Map<String, Object> workflow, VideoSettingsPayload settings) {
        Map<String, Object> inputs = ComfyHelper.getStringObjectMap(workflow, "72");
        inputs.put("text", settings.negativePrompt());

        inputs = ComfyHelper.getStringObjectMap(workflow, "89");
        inputs.put("text", settings.positivePrompt());
    }

    private ComfyuiService.GeneratedFile withVideoFilename(ComfyuiService.GeneratedFile file) {
        String filename = file.filename();

        if (filename == null || filename.isBlank()) {
            filename = UUID.randomUUID() + ".mp4";
        } else if (!filename.contains(".")) {
            filename = filename + ".mp4";
        }

        return new ComfyuiService.GeneratedFile(
                file.promptId(),
                filename,
                file.subfolder(),
                file.type(),
                file.bytes()
        );
    }
}