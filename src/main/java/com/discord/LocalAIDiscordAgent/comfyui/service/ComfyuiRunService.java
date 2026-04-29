//package com.discord.LocalAIDiscordAgent.comfyui.service;
//
//import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.service.ImageGenerationService;
//import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.service.MusicGenerationService;
//import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.service.VideoGenerationService;
//import com.discord.LocalAIDiscordAgent.llm.llmChains.data.PromptData;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//
//@Slf4j
//@Service
//public class ComfyuiRunService {
//
//    private final MusicGenerationService musicGenerationService;
//    private final ImageGenerationService imageGenerationService;
//    private final VideoGenerationService videoGenerationService;
//
//    public ComfyuiRunService(
//            MusicGenerationService musicGenerationService,
//            ImageGenerationService imageGenerationService,
//            VideoGenerationService videoGenerationService
//    ) {
//        this.musicGenerationService = musicGenerationService;
//        this.imageGenerationService = imageGenerationService;
//        this.videoGenerationService = videoGenerationService;
//    }
//
//    public Mono<ComfyuiService.GeneratedFile> generateMusic(PromptData promptData) {
//        return musicGenerationService.generateMusic(promptData);
//    }
//
//    public Mono<ComfyuiService.GeneratedFile> generateImage(PromptData promptData) {
//        return imageGenerationService.generateImage(promptData);
//    }
//
//    public Mono<ComfyuiService.GeneratedFile> generateVideo(PromptData promptData) {
//        return videoGenerationService.generateVideo(promptData);
//    }
//}