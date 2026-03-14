package com.discord.LocalAIDiscordAgent.discord.config;

import com.discord.LocalAIDiscordAgent.discord.listener.EventListenerINTF;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BotConfiguration {

    @Value("${discord.token}")
    private String token;

    @Bean
    public <T extends Event> GatewayDiscordClient gatewayDiscordClient(List<EventListenerINTF<T>> eventListenerINTFS) {

        GatewayDiscordClient discord  = DiscordClientBuilder.create(token)
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.of(
                        Intent.GUILDS,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.MESSAGE_CONTENT,  // Privileged intent
                        Intent.GUILD_MEMBERS,    // Privileged intent (if needed)
                        Intent.GUILD_PRESENCES   // Privileged intent (if needed)
                ))
                .login()
                .block();


        for(EventListenerINTF<T> listener : eventListenerINTFS) {
            discord.on(listener.getEventType())
                    .flatMap(listener::execute)
                    .onErrorResume(listener::handleError)
                    .subscribe();
        }


        return discord;
    }

}
