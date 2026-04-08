package com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.service;

import com.discord.LocalAIDiscordAgent.comfyui.helpers.ComfyHelper;
import com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.records.MusicSettingsRecord;
import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
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
public class MusicGenerationService {

    private final ObjectMapper objectMapper;
    private final ComfyuiService comfyuiService;
    private static final Path FILE_ROOT = Path.of("output/music");

    public MusicGenerationService(ObjectMapper objectMapper, ComfyuiService comfyuiService) {
        this.objectMapper = objectMapper;
        this.comfyuiService = comfyuiService;
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

        setMusicSettings(apiWorkflow,  musicSettings);

        byte[] audioBytes = comfyuiService.runGenerationWorkflow(apiWorkflow);

        String fileName = musicSettings.title() + " "+ UUID.randomUUID() + ".mp3".trim();
        Path outputPath = FILE_ROOT.resolve(fileName).normalize();

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        Files.write(outputPath, audioBytes);

        return outputPath;
    }

    private void setMusicSettings(Map<String, Object> workflow, MusicSettingsRecord settings) {
        Map<String, Object> inputs = ComfyHelper.getStringObjectMap(workflow, "94");

        inputs.put("keyscale", settings.keyscale());
        inputs.put("lyrics", settings.lyrics());
        inputs.put("bpm", settings.bpm());
        inputs.put("tags", settings.tags());
        inputs.put("duration", settings.duration());

        inputs = ComfyHelper.getStringObjectMap(workflow, "98");
        inputs.put("seconds", settings.duration());

    }

}
