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
    public GatewayDiscordClient gatewayDiscordClient(List<EventListenerINTF<? extends Event>> listeners) {
        GatewayDiscordClient discord = DiscordClientBuilder.create(token)
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.of(
                        Intent.GUILDS,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.MESSAGE_CONTENT,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_PRESENCES
                ))
                .login()
                .block();

        if (discord == null) {
            throw new IllegalStateException("Failed to login to Discord gateway.");
        }

        for (EventListenerINTF<? extends Event> listener : listeners) {
            registerListener(discord, listener);
        }

        return discord;
    }

    private <T extends Event> void registerListener(GatewayDiscordClient discord, EventListenerINTF<T> listener) {
        discord.on(listener.getEventType())
                .flatMap(listener::execute)
                .onErrorResume(listener::handleError)
                .subscribe();
    }
}
