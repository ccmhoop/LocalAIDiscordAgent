package com.discord.LocalAIDiscordAgent.comfyui.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ComfyUiConfig {

    @Bean
    RestClient comfyUiRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        return RestClient.builder()
                .baseUrl("http://127.0.0.1:8000")
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }
}