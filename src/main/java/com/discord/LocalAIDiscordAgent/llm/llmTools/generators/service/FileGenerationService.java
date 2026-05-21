package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.service;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService.GeneratedFile;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.service.ImageGenerationService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.service.MusicGenerationService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.service.VideoGenerationService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FileGenerationService {

    private final ImageGenerationService imageGenerationService;
    private final VideoGenerationService videoGenerationService;
    private final MusicGenerationService musicGenerationService;

    public FileGenerationService(
            ImageGenerationService imageGenerationService,
            VideoGenerationService videoGenerationService,
            MusicGenerationService musicGenerationService
    ) {
        this.imageGenerationService = imageGenerationService;
        this.videoGenerationService = videoGenerationService;
        this.musicGenerationService = musicGenerationService;
    }

    public Mono<GeneratedFile> imageGenerationChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        return imageGenerationService.executeLLMChain(discGlobalData, requiresContext);
    }

    public Mono<ComfyuiService.GeneratedFile> videoGenerationChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        return videoGenerationService.executeLLMChain(discGlobalData, requiresContext);
    }

    public Mono<ComfyuiService.GeneratedFile> musicGenerationChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        return musicGenerationService.executeLLMChain(discGlobalData, requiresContext);
    }

}
