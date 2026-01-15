package com.discord.LocalAIDiscordAgent.aiAdvisor.config;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AdvisorConfig {

    @Bean
    public List<Advisor> scottishAdvisorConfig(List<Advisor> scottishAdvisorsList) {
        return scottishAdvisorsList;
    }

}
