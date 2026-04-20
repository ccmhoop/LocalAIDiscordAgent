package com.discord.LocalAIDiscordAgent.comfyui.generators.imageGenerator.service;

import com.discord.LocalAIDiscordAgent.comfyui.generators.imageGenerator.payloadRecord.ImageSettingsPayload;
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
public class ImageGenerationService {

    private final ObjectMapper objectMapper;
    private final ComfyuiService comfyuiService;

    public ImageGenerationService(ObjectMapper objectMapper, ComfyuiService comfyuiService) {
        this.objectMapper = objectMapper;
        this.comfyuiService = comfyuiService;
    }

    public Mono<ComfyuiService.GeneratedFile> generateImage(PromptData promptData) {
        return Mono.fromCallable(() -> buildWorkflow(promptData))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(comfyuiService::runGenerationWorkflow)
                .map(this::withImageFilename);
    }

    private Map<String, Object> buildWorkflow(PromptData promptData) throws Exception {
        ImageSettingsPayload imageSettings = promptData.getImageSettings();
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
        return apiWorkflow;
    }

    private void setImageSettings(Map<String, Object> workflow, ImageSettingsPayload settings) {
        Map<String, Object> inputs = ComfyHelper.getStringObjectMap(workflow, "5");
        inputs.put("width", settings.pixelWidth());
        inputs.put("height", settings.pixelHeight());

        inputs = ComfyHelper.getStringObjectMap(workflow, "6");
        inputs.put("text", settings.positivePrompt());

        inputs = ComfyHelper.getStringObjectMap(workflow, "7");
        inputs.put("text", settings.negativePrompt());
    }

    private ComfyuiService.GeneratedFile withImageFilename(ComfyuiService.GeneratedFile file) {
        String filename = file.filename();

        if (filename == null || filename.isBlank()) {
            filename = UUID.randomUUID() + ".png";
        } else if (!filename.contains(".")) {
            filename = filename + ".png";
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