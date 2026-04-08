package com.discord.LocalAIDiscordAgent.discord.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalDataContextHolder;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalDataService;
import com.discord.LocalAIDiscordAgent.discord.listener.EventListenerINTF;
import com.discord.LocalAIDiscordAgent.discord.listener.MessageListener;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessSummaryClient;
import com.discord.LocalAIDiscordAgent.llmClients.chatClient.service.ChatClientService;
import com.discord.LocalAIDiscordAgent.user.service.UserService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
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
        Message message = event.getMessage();

        if (message.getAuthor().map(User::isBot).orElse(false)) {
            return Mono.empty();
        }

        Snowflake botId = event.getClient().getSelfId();

        if (!message.getUserMentionIds().contains(botId)) {
            return Mono.empty();
        }

        if (!startsWithBotMention(message.getContent(), botId)) {
            return Mono.empty();
        }

        DiscGlobalData discGlobalData = discGlobalDataService.build(event);

        if (!discGlobalData.isValid()) {
            return Mono.empty();
        }

        if (discGlobalData.hasEmptyPrompt()) {
            return message.getChannel()
                    .flatMap(channel -> channel.createMessage(
                            "You mentioned me, but your message is empty. Please add a question or instruction after the mention."
                    ))
                    .then();
        }

        return processCommandAI(
                message,
                userService,
                chatClientService,
                processSummaryClient
        ).contextWrite(ctx -> DiscGlobalDataContextHolder.put(ctx, discGlobalData));
    }

    private boolean startsWithBotMention(String content, Snowflake botId) {
        if (content == null || content.isBlank()) {
            return false;
        }

        String trimmed = content.trim();
        String mention1 = "<@" + botId.asString() + ">";
        String mention2 = "<@!" + botId.asString() + ">";

        return trimmed.startsWith(mention1) || trimmed.startsWith(mention2);
    }
}