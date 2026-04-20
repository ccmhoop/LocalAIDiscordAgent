package com.discord.LocalAIDiscordAgent.comfyui.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ComfyUiConfig {

    @Bean
    public HttpClient comfyUiHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public RestClient comfyUiRestClient(
            HttpClient comfyUiHttpClient,
            @Value("${comfyui.http-base-url}") String httpBaseUrl
    ) {
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(comfyUiHttpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .baseUrl(httpBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}