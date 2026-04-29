package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.fileGeneration;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.dto.MusicSettingsDTO;
import com.discord.LocalAIDiscordAgent.comfyui.helpers.ComfyHelper;
import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.UUID;

@Component
public class MusicFileGeneration {

    private final ObjectMapper objectMapper;
    private final ComfyuiService comfyuiService;

    public MusicFileGeneration(ObjectMapper objectMapper, ComfyuiService comfyuiService) {
        this.objectMapper = objectMapper;
        this.comfyuiService = comfyuiService;
    }

    public Mono<ComfyuiService.GeneratedFile> generateMusicFile(PromptData promptData) {
        return Mono.fromCallable(() -> buildWorkflow(promptData))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(comfyuiService::runGenerationWorkflow)
                .map(file -> withMusicFilename(file, promptData.getMusicSettings()));
    }

    private Map<String, Object> buildWorkflow(PromptData promptData) throws Exception {
        MusicSettingsDTO musicSettings = promptData.getMusicSettings();
        ClassPathResource workflowResource = new ClassPathResource("comfyui/music_gen_api.json");

        if (!workflowResource.exists()) {
            throw new IllegalStateException("Workflow file not found in classpath: comfyui/music_gen_api.json");
        }

        Map<String, Object> apiWorkflow;
        try (var inputStream = workflowResource.getInputStream()) {
            apiWorkflow = objectMapper.readValue(
                    inputStream,
                    new TypeReference<Map<String, Object>>() {}
            );
        }

        setMusicSettings(apiWorkflow, musicSettings);
        return apiWorkflow;
    }

    private void setMusicSettings(Map<String, Object> workflow, MusicSettingsDTO settings) {
        Map<String, Object> inputs = ComfyHelper.getStringObjectMap(workflow, "94");

        inputs.put("keyscale", settings.keyscale());
        inputs.put("lyrics", settings.lyrics());
        inputs.put("bpm", settings.bpm());
        inputs.put("tags", settings.tags());
        inputs.put("duration", settings.duration());

        inputs = ComfyHelper.getStringObjectMap(workflow, "98");
        inputs.put("seconds", settings.duration());
    }

    private ComfyuiService.GeneratedFile withMusicFilename(
            ComfyuiService.GeneratedFile file,
            MusicSettingsDTO settings
    ) {
        String baseName = sanitizeFilename(settings.title());
        if (baseName == null || baseName.isBlank()) {
            baseName = "music-" + UUID.randomUUID();
        }

        String filename = baseName + ".mp3";

        return new ComfyuiService.GeneratedFile(
                file.promptId(),
                filename,
                file.subfolder(),
                file.type(),
                file.bytes()
        );
    }

    private String sanitizeFilename(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = value.replaceAll("[\\\\/:*?\"<>|]+", " ").trim();
        sanitized = sanitized.replaceAll("\\s+", "-");

        if (sanitized.length() > 80) {
            sanitized = sanitized.substring(0, 80);
        }

        return sanitized;
    }
}