package com.discord.LocalAIDiscordAgent.comfyui.service;

import com.discord.LocalAIDiscordAgent.comfyui.imageGenerator.service.ImageGenerationService;
import com.discord.LocalAIDiscordAgent.comfyui.musicGenerator.service.MusicGenerationService;
import com.discord.LocalAIDiscordAgent.comfyui.videoGenerator.service.VideoGenerationService;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Slf4j
@Service
public class ComfyuiRunService {

    private final MusicGenerationService musicGenerationService;
    private final ImageGenerationService imageGenerationService;
    private final VideoGenerationService videoGenerationService;

    public ComfyuiRunService(
            MusicGenerationService musicGenerationService,
            ImageGenerationService imageGenerationService,
            VideoGenerationService videoGenerationService) {
        this.musicGenerationService = musicGenerationService;
        this.imageGenerationService = imageGenerationService;
        this.videoGenerationService = videoGenerationService;
    }

    public Path generateMusic(PromptData promptData) throws Exception {
       return musicGenerationService.generateMusic(promptData);
    }

    public Path generateImage(PromptData promptData) throws Exception{
        return imageGenerationService.generateImage(promptData);
    }

    public Path generateVideo(PromptData promptData) throws Exception{
        return videoGenerationService.generateVideo(promptData);
    }

}
