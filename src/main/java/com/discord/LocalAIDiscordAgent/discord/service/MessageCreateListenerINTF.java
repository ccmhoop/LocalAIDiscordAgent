package com.discord.LocalAIDiscordAgent.discord.service;

import com.discord.LocalAIDiscordAgent.chatClient.service.ToolClientService;
import com.discord.LocalAIDiscordAgent.discord.listener.EventListenerINTF;
import com.discord.LocalAIDiscordAgent.discord.listener.MessageListener;
import com.discord.LocalAIDiscordAgent.chatClient.service.ChatClientService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class MessageCreateListenerINTF extends MessageListener implements EventListenerINTF<MessageCreateEvent> {

    private final ChatClientService chatClientService;
    private final ToolClientService toolClientService;
    public MessageCreateListenerINTF(ChatClientService chatClientService, ToolClientService toolClientService) {
        this.chatClientService = chatClientService;
        this.toolClientService = toolClientService;
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        return processCommandAI(event.getMessage(), chatClientService, toolClientService);
    }
}
