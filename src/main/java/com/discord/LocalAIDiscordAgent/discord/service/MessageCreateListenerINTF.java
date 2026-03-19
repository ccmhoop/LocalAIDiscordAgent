package com.discord.LocalAIDiscordAgent.discord.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
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
    private final DiscGlobalData discGlobalData;
    private final ChatClientService chatClientService;
    private final ProcessSummaryClient processSummaryClient;

    public MessageCreateListenerINTF(
            UserService userService,
            DiscGlobalData discGlobalData,
            ChatClientService chatClientService,
            ProcessSummaryClient processSummaryClient
    ) {
        this.processSummaryClient = processSummaryClient;
        this.chatClientService = chatClientService;
        this.discGlobalData = discGlobalData;
        this.userService = userService;

    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {

        discGlobalData.setDiscData(event);

        if (discGlobalData.dataIsEmptyOrNull() || discGlobalData.getUserMessage().isBlank()){
            return Mono.empty();
        }


        return processCommandAI(event.getMessage(), discGlobalData, userService, chatClientService, processSummaryClient);
    }

}
