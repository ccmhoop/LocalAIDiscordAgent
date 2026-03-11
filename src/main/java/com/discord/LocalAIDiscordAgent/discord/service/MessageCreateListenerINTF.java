package com.discord.LocalAIDiscordAgent.discord.service;

import com.discord.LocalAIDiscordAgent.chatClient.service.ToolClientService;
import com.discord.LocalAIDiscordAgent.discord.listener.EventListenerINTF;
import com.discord.LocalAIDiscordAgent.discord.listener.MessageListener;
import com.discord.LocalAIDiscordAgent.chatClient.service.ChatClientService;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessSummaryClient;
import com.discord.LocalAIDiscordAgent.user.service.UserService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class MessageCreateListenerINTF extends MessageListener implements EventListenerINTF<MessageCreateEvent> {

    private final UserService userService;
    private final ChatClientService chatClientService;
    private final ToolClientService toolClientService;
    private final ProcessSummaryClient processSummaryClient;

    public MessageCreateListenerINTF(UserService userService, ChatClientService chatClientService, ToolClientService toolClientService, ProcessSummaryClient processSummaryClient) {
        this.userService = userService;
        this.chatClientService = chatClientService;
        this.toolClientService = toolClientService;
        this.processSummaryClient = processSummaryClient;
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        return processCommandAI(event.getMessage(), userService, chatClientService, toolClientService, processSummaryClient);
    }

}
