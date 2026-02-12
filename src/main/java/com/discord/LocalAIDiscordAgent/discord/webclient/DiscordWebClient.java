package com.discord.LocalAIDiscordAgent.discord.webclient;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.TextChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DiscordWebClient {

    private final GatewayDiscordClient client;

    @PostConstruct
    public void init() {
//        sendMessageToChannel("Stock Tracker started", "general");
    }

    public void sendMessageToChannel(String message, String channelName) {
        client.getGuilds()
                .flatMap(guild -> guild.getChannels()
                        .ofType(TextChannel.class)
                        .filter(channel -> channel.getName().equalsIgnoreCase(channelName))
                        .flatMap(channel -> channel.createMessage(message)))
                .subscribe(
                        msg -> log.info("Sent message to {} channel: {}", channelName, message),
                        error -> log.error("Error sending message to {} channel: {}", channelName, error.getMessage())
                );
    }

}
