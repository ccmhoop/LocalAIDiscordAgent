package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.service;

import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService;
import com.discord.LocalAIDiscordAgent.comfyui.service.ComfyuiService.GeneratedFile;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.service.ImageGenerationChainService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.service.MusicGenerationChainService;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.service.VideoGenerationChainService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FileGeneratorLLMChainService {

    private final ImageGenerationChainService imageGenerationChainService;
    private final VideoGenerationChainService videoGenerationChainService;
    private final MusicGenerationChainService musicGenerationChainService;

    public FileGeneratorLLMChainService(
            ImageGenerationChainService imageGenerationChainService,
            VideoGenerationChainService videoGenerationChainService,
            MusicGenerationChainService musicGenerationChainService
    ) {
        this.imageGenerationChainService = imageGenerationChainService;
        this.videoGenerationChainService = videoGenerationChainService;
        this.musicGenerationChainService = musicGenerationChainService;
    }

    public Mono<GeneratedFile> imageGenerationLLMChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        return imageGenerationChainService.executeLLMChain(discGlobalData, requiresContext);
    }

    public Mono<ComfyuiService.GeneratedFile> videoGenerationLLMChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        return videoGenerationChainService.executeLLMChain(discGlobalData, requiresContext);
    }

    public Mono<ComfyuiService.GeneratedFile> musicGenerationLLMChain(DiscGlobalData discGlobalData, boolean requiresContext) {
        return musicGenerationChainService.executeLLMChain(discGlobalData, requiresContext);
    }

}
