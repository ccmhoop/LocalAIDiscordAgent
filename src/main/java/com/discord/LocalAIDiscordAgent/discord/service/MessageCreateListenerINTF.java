package com.discord.LocalAIDiscordAgent.discord.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalDataContextHolder;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalDataService;
import com.discord.LocalAIDiscordAgent.discord.listener.EventListenerINTF;
import com.discord.LocalAIDiscordAgent.discord.listener.MessageListener;
import com.discord.LocalAIDiscordAgent.llmClients.chatClient.service.ChatClientService;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.llmCallChains.LLMCallChain;
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
    private final DiscGlobalDataService discGlobalDataService;
    private final DiscordRequestQueueService discordRequestQueueService;
    private final LLMCallChain llmCallChain;

    public MessageCreateListenerINTF(
            UserService userService,
            ChatClientService chatClientService,
            DiscGlobalDataService discGlobalDataService,
            DiscordRequestQueueService discordRequestQueueService,
            LLMCallChain llmCallChain
    ) {
        this.userService = userService;
        this.chatClientService = chatClientService;
        this.discGlobalDataService = discGlobalDataService;
        this.discordRequestQueueService = discordRequestQueueService;
        this.llmCallChain = llmCallChain;
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        return Mono.defer(() -> {
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

            return discGlobalDataService.build(event)
                    .flatMap(discGlobalData -> handleBuiltDiscData(message, discGlobalData))
                    .switchIfEmpty(Mono.empty());
        });
    }

    private Mono<Void> handleBuiltDiscData(Message message, DiscGlobalData discGlobalData) {
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

        String requestId = message.getId().asString();

        return discordRequestQueueService.enqueue(
                requestId,
                () -> processCommandAI(
                        message,
                        userService,
                        chatClientService,
                        llmCallChain
                ).contextWrite(ctx -> DiscGlobalDataContextHolder.put(ctx, discGlobalData))
        );
    }

    @Override
    public Mono<Void> handleError(Throwable error) {
        log.error("Unhandled error in MessageCreateListenerINTF", error);
        return Mono.empty();
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