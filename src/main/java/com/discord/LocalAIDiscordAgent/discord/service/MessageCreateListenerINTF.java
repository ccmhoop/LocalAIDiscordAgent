package com.discord.LocalAIDiscordAgent.discord.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalDataContextHolder;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalDataService;
import com.discord.LocalAIDiscordAgent.discord.listener.EventListenerINTF;
import com.discord.LocalAIDiscordAgent.discord.listener.MessageListener;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessSummaryClient;
import com.discord.LocalAIDiscordAgent.llmClients.chatClient.service.ChatClientService;
import com.discord.LocalAIDiscordAgent.user.service.UserService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class MessageCreateListenerINTF extends MessageListener implements EventListenerINTF<MessageCreateEvent> {

    private final UserService userService;
    private final ChatClientService chatClientService;
    private final ProcessSummaryClient processSummaryClient;
    private final DiscGlobalDataService discGlobalDataService;

    public MessageCreateListenerINTF(
            UserService userService,
            ChatClientService chatClientService,
            ProcessSummaryClient processSummaryClient,
            DiscGlobalDataService discGlobalDataService
    ) {
        this.userService = userService;
        this.chatClientService = chatClientService;
        this.processSummaryClient = processSummaryClient;
        this.discGlobalDataService = discGlobalDataService;
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        DiscGlobalData discGlobalData = discGlobalDataService.build(event);

        if (discGlobalData == null) {
            return Mono.empty();
        }

        return processCommandAI(
                event.getMessage(),
                userService,
                chatClientService,
                processSummaryClient
        ).contextWrite(ctx -> DiscGlobalDataContextHolder.put(ctx, discGlobalData));
    }
}